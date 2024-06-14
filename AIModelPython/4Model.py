import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix

# Load the datasets
stress_data = pd.read_csv('8_5_stressLevel.csv')
hr_data = pd.read_csv('8_5_heartRate.csv')

# Keep only relevant columns
hr_data_cleaned = hr_data[['timestamp', 'heart rate']].dropna()
stress_data_cleaned = stress_data[['stress level value', 'stress level time']]

# Convert timestamps to datetime for better handling
hr_data_cleaned['timestamp'] = pd.to_datetime(hr_data_cleaned['timestamp'])
stress_data_cleaned['stress level time'] = pd.to_datetime(stress_data_cleaned['stress level time'])

# Interpolate missing heart rate values
hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], errors='coerce')
hr_data['heart rate'] = hr_data['heart rate'].interpolate(method='linear')

# Drop rows where the 'timestamp' is still NaN after coercion
hr_data_cleaned = hr_data.dropna(subset=['timestamp', 'heart rate'])

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
y = merged_data['stress level value'] > 70  # Define stress as levels greater than 15 (binary classification)

# Split data into training and testing sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Standardize the features
scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Train a RandomForest model
model = RandomForestClassifier(random_state=42)
model.fit(X_train_scaled, y_train)

# Predict and evaluate the model
y_pred = model.predict(X_test_scaled)

print("Classification Report:")
print(classification_report(y_test, y_pred))

print("Confusion Matrix:")
print(confusion_matrix(y_test, y_pred))
