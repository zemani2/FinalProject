import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
import joblib

# Load the data
def load_data(hr_path, stress_path):
    hr_data = pd.read_csv(hr_path)
    stress_data = pd.read_csv(stress_path)

    # Fill forward the timestamps
    hr_data['timestamp'] = hr_data['timestamp'].ffill()
    hr_data['timestamp 16'] = hr_data['timestamp 16'].fillna(0)

    # Convert the original timestamp to datetime
    hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], format='%Y-%m-%dT%H:%M:%S.%fZ')

    # Compute the mesgTimestamp using the provided formula
    def compute_mesgTimestamp(row):
        base_timestamp = int(row['timestamp'].value // 10 ** 6)  # Convert to milliseconds and then to integer
        timestamp_16 = int(row['timestamp 16'])  # Handle NaN by setting to 0
        mesg_timestamp = base_timestamp + ((timestamp_16 - (base_timestamp & 0xFFFF)) & 0xFFFF)
        return pd.to_datetime(mesg_timestamp, unit='ms')

    hr_data['mesgTimestamp'] = hr_data.apply(compute_mesgTimestamp, axis=1)

    # Select only the necessary columns
    hr_data = hr_data[['mesgTimestamp', 'heart rate']]
    hr_data = hr_data.set_index('mesgTimestamp')

    # Resample the heart rate data to ensure each minute has a value
    hr_data = hr_data.resample('min').mean().ffill()

    # Convert the stress level time to datetime
    stress_data['stress level time'] = pd.to_datetime(stress_data['stress level time'], utc=True)

    # Resample the stress data to minute-level frequency, assuming the values are constant over each minute
    stress_data = stress_data.set_index('stress level time').resample('min').ffill()

    # Ensure both indexes are in UTC
    hr_data.index = hr_data.index.tz_convert('UTC')
    stress_data.index = stress_data.index.tz_convert('UTC')

    # Merge HR and stress data on the minute-level timestamp
    data = pd.merge_asof(hr_data, stress_data, left_index=True, right_index=True)

    # Generate a complete datetime range for minute-level resampling
    full_index = pd.date_range(start=data.index.min(), end=data.index.max(), freq='min', tz='UTC')
    data = data.reindex(full_index).ffill()

    # Drop unnecessary columns and rows with NaN values
    data = data[['heart rate', 'stress level value']].dropna()

    # Define the target variable 'anxiety'
    data['anxiety'] = data['stress level value'] > 70  # Example threshold
    return data

# Prepare data for LSTM
def prepare_data(data, sequence_length=5):
    X, y = [], []
    for i in range(len(data) - sequence_length):
        X.append(data['heart rate'].values[i:i + sequence_length])
        y.append(data['anxiety'].values[i + sequence_length])
    return np.array(X), np.array(y)

# Define the LSTM model
class LSTMModel(nn.Module):
    def __init__(self, input_size, hidden_layer_size, output_size):
        super(LSTMModel, self).__init__()
        self.hidden_layer_size = hidden_layer_size
        self.lstm = nn.LSTM(input_size, hidden_layer_size)
        self.linear = nn.Linear(hidden_layer_size, output_size)
        self.hidden_cell = (torch.zeros(1, 1, self.hidden_layer_size),
                            torch.zeros(1, 1, self.hidden_layer_size))

    def forward(self, input_seq):
        lstm_out, self.hidden_cell = self.lstm(input_seq.view(len(input_seq), 1, -1), self.hidden_cell)
        predictions = self.linear(lstm_out.view(len(input_seq), -1))
        return predictions[-1]

# Train the model
def train_model(hr_path, stress_path):
    data = load_data(hr_path, stress_path)
    X, y = prepare_data(data)

    # Scale the data
    scaler = StandardScaler()
    X = scaler.fit_transform(X.reshape(-1, X.shape[-1])).reshape(X.shape)

    # Convert to PyTorch tensors
    X = torch.tensor(X, dtype=torch.float32)
    y = torch.tensor(y, dtype=torch.float32)

    # Split the data
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    # Create DataLoader
    train_data = TensorDataset(X_train, y_train)
    test_data = TensorDataset(X_test, y_test)
    train_loader = DataLoader(train_data, batch_size=32, shuffle=True)
    test_loader = DataLoader(test_data, batch_size=32, shuffle=False)

    # Initialize the model, loss function, and optimizer
    model = LSTMModel(input_size=X_train.shape[2], hidden_layer_size=50, output_size=1)
    loss_function = nn.BCEWithLogitsLoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    # Train the model
    epochs = 10
    for epoch in range(epochs):
        for inputs, labels in train_loader:
            model.hidden_cell = (torch.zeros(1, 1, model.hidden_layer_size),
                                 torch.zeros(1, 1, model.hidden_layer_size))

            model.zero_grad()
            outputs = model(inputs)
            loss = loss_function(outputs.squeeze(), labels)
            loss.backward()
            optimizer.step()

        print(f'Epoch {epoch + 1}/{epochs} - Loss: {loss.item()}')

    # Save the model and scaler
    torch.save(model.state_dict(), 'anxiety_lstm_model.pth')
    joblib.dump(scaler, 'scaler.pkl')
    return model, scaler

# Predict using the model
def predict_anxiety(heart_rate_packet):
    model = LSTMModel(input_size=len(heart_rate_packet), hidden_layer_size=50, output_size=1)
    model.load_state_dict(torch.load('anxiety_lstm_model.pth'))
    model.eval()

    scaler = joblib.load('scaler.pkl')
    heart_rate_packet = np.array(heart_rate_packet).reshape(1, -1)
    heart_rate_packet = scaler.transform(heart_rate_packet.reshape(-1, heart_rate_packet.shape[-1])).reshape(
        heart_rate_packet.shape)

    heart_rate_packet = torch.tensor(heart_rate_packet, dtype=torch.float32)
    with torch.no_grad():
        model.hidden_cell = (torch.zeros(1, 1, model.hidden_layer_size),
                             torch.zeros(1, 1, model.hidden_layer_size))
        prediction = torch.sigmoid(model(heart_rate_packet))

    return prediction.item() > 0.5

if __name__ == "__main__":
    heart_rate_file_name = "8_5_heartRate.csv"
    stress_level_file_name = "8_5_stressLevel.csv"
    # Train the model
    train_model(heart_rate_file_name, stress_level_file_name)

    # Example prediction
    heart_rate_packet = [55, 57, 55, 53, 54]  # Example heart rate packet
    is_anxious = predict_anxiety(heart_rate_packet)
    print("Is Anxious:", is_anxious)
