import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score

class StressPredictor:
    def __init__(self):
        self.model = RandomForestClassifier(random_state=42)
        self.scaler = StandardScaler()

    def load_data(self, stress_files, hr_files):
        all_stress_data = []
        all_hr_data = []

        for stress_file in stress_files:
            stress_data = pd.read_csv(stress_file)
            all_stress_data.append(stress_data)

        for hr_file in hr_files:
            hr_data = pd.read_csv(hr_file)
            all_hr_data.append(hr_data)

        self.stress_data = pd.concat(all_stress_data)
        self.hr_data = pd.concat(all_hr_data)

    def preprocess_data(self):
        # Keep only relevant columns
        hr_data_cleaned = self.hr_data[['timestamp', 'heart rate']].dropna()
        stress_data_cleaned = self.stress_data[['stress level value', 'stress level time']]

        # Convert timestamps to datetime for better handling
        hr_data_cleaned['timestamp'] = pd.to_datetime(hr_data_cleaned['timestamp'])
        stress_data_cleaned['stress level time'] = pd.to_datetime(stress_data_cleaned['stress level time'])

        # Interpolate missing heart rate values
        self.hr_data['timestamp'] = pd.to_datetime(self.hr_data['timestamp'], errors='coerce')
        self.hr_data['heart rate'] = self.hr_data['heart rate'].interpolate(method='linear')

        # Drop rows where the 'timestamp' is still NaN after coercion
        hr_data_cleaned = self.hr_data.dropna(subset=['timestamp', 'heart rate'])

        # Keep only relevant columns
        hr_data_cleaned = hr_data_cleaned[['timestamp', 'heart rate']]

        # Ensure that both datasets have timestamps in the same format
        hr_data_cleaned['timestamp'] = pd.to_datetime(hr_data_cleaned['timestamp'])
        stress_data_cleaned['stress level time'] = pd.to_datetime(stress_data_cleaned['stress level time'])

        # Resample heart rate data to ensure one value per minute
        hr_data_resampled = hr_data_cleaned.set_index('timestamp').resample('min').mean().reset_index()

        # Forward fill missing heart rate values
        hr_data_resampled['heart rate'] = hr_data_resampled['heart rate'].ffill()

        # Merge stress data with heart rate data based on the timestamp
        merged_data = pd.merge(
            hr_data_resampled,
            stress_data_cleaned,
            left_on='timestamp',
            right_on='stress level time',
            how='inner'
        ).drop(columns=['stress level time'])

        # Feature Engineering
        # Create rolling averages and differences as features
        merged_data['hr_rolling_mean_5'] = merged_data['heart rate'].rolling(window=5).mean().shift(1)
        merged_data['hr_diff'] = merged_data['heart rate'].diff().shift(1)
        merged_data = merged_data.dropna()

        # Define features and target variable
        X = merged_data[['heart rate', 'hr_rolling_mean_5', 'hr_diff']]
        y = merged_data['stress level value'] > 15  # Define stress as levels greater than 15 (binary classification)

        return X, y

    def train_model(self, X, y):
        # Split data into training and testing sets
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        # Standardize the features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)

        # Train the RandomForest model
        self.model.fit(X_train_scaled, y_train)

        # Evaluate the model
        y_pred = self.model.predict(X_test_scaled)
        print("Classification Report:")
        print(classification_report(y_test, y_pred))
        print("Confusion Matrix:")
        print(confusion_matrix(y_test, y_pred))
        print("Accuracy:", accuracy_score(y_test, y_pred))

    def predict_stress(self, hr_block):
        # Ensure hr_block is a DataFrame with the correct columns
        if isinstance(hr_block, pd.DataFrame) and 'heart rate' in hr_block.columns:
            # Create features from hr_block
            hr_block['hr_rolling_mean_5'] = hr_block['heart rate'].rolling(window=5).mean().shift(1)
            hr_block['hr_diff'] = hr_block['heart rate'].diff().shift(1)
            hr_block = hr_block.dropna()

            if len(hr_block) < 1:
                raise ValueError("hr_block must have enough data points to create rolling features")

            # Standardize the features
            X_block_scaled = self.scaler.transform(hr_block[['heart rate', 'hr_rolling_mean_5', 'hr_diff']])

            # Predict stress levels
            predictions = self.model.predict(X_block_scaled)

            # Convert predictions to Yes/No
            return ["Yes" if pred else "No" for pred in predictions]
        else:
            raise ValueError("hr_block must be a DataFrame with a 'heart rate' column")

# Example usage:
stress_files = ['5_5_stressLevel.csv', '9_5_stressLevel.csv', '8_5_stressLevel.csv']
hr_files = ['5_5_heartRate.csv', '9_5_heartRate.csv', '8_5_heartRate.csv']

predictor = StressPredictor()
predictor.load_data(stress_files, hr_files)
X, y = predictor.preprocess_data()
predictor.train_model(X, y)

hr_block = pd.DataFrame({'heart rate': [56, 57, 60, 68, 78, 68, 65, 60]})
predictions = predictor.predict_stress(hr_block)
print(predictions)
