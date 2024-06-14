# Example usage
stress_files = ['data/9_5_stressLevel.csv', 'data/8_5_stressLevel.csv', 'data/hjhtkr-stressLevel.csv', 'data/dovevshachar-stressLevel.csv']
hr_files = ['data/9_5_heartRate.csv', 'data/8_5_heartRate.csv', 'data/hjhtkr-heartRate.csv', 'data/dovevshachar-heartRate.csv']
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score


class StressModelBuilder:
    def __init__(self):
        self.model = RandomForestClassifier(random_state=42)
        self.scaler = StandardScaler()

    def load_and_preprocess_data(self, stress_files, hr_files):
        all_stress_data = []
        all_hr_data = []

        for stress_file in stress_files:
            stress_data = pd.read_csv(stress_file)
            all_stress_data.append(stress_data)

        for hr_file in hr_files:
            hr_data = pd.read_csv(hr_file)
            all_hr_data.append(hr_data)

        stress_data = pd.concat(all_stress_data)
        hr_data = pd.concat(all_hr_data)

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

        # Drop rows with NaN values in stress level time
        stress_data_cleaned = stress_data_cleaned.dropna(subset=['stress level time'])

        # Merge the datasets on the closest timestamp
        merged_data = pd.merge_asof(stress_data_cleaned.sort_values('stress level time'),
                                    hr_data_cleaned.sort_values('timestamp'),
                                    left_on='stress level time',
                                    right_on='timestamp',
                                    direction='nearest')

        # Create features
        merged_data['hr_rolling_mean_5'] = merged_data['heart rate'].rolling(window=5).mean().shift(1)
        merged_data['hr_diff'] = merged_data['heart rate'].diff().shift(1)

        # Drop rows with NaN values created by rolling and diff
        merged_data.dropna(inplace=True)

        X = merged_data[['heart rate', 'hr_rolling_mean_5', 'hr_diff']]
        y = merged_data['stress level value'] > 50  # Assuming stress level above 50 indicates stress

        return X, y

    def train_model(self, X, y):
        # Split the data
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

    def predict_stress_hours(self, hr_file):
        # Load heart rate data from file
        hr_data = pd.read_csv(hr_file)

        # Ensure hr_data is a DataFrame with the correct columns
        if 'timestamp' in hr_data.columns and 'heart rate' in hr_data.columns:
            # Convert timestamp to datetime
            hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], errors='coerce')

            if len(hr_data) < 6:
                raise ValueError("hr_data must have at least 6 data points to create rolling features")

            # Create features from hr_data
            hr_data['hr_rolling_mean_5'] = hr_data['heart rate'].rolling(window=5, min_periods=1).mean().shift(1)
            hr_data['hr_diff'] = hr_data['heart rate'].diff().shift(1)
            hr_data = hr_data.dropna()

            if len(hr_data) < 1:
                raise ValueError("hr_data must have enough data points to create rolling features after rolling and diff")

            # Standardize the features
            X_block_scaled = self.scaler.transform(hr_data[['heart rate', 'hr_rolling_mean_5', 'hr_diff']])

            # Predict stress levels
            predictions = self.model.predict(X_block_scaled)

            # Get the timestamps where stress is predicted
            stress_times = hr_data['timestamp'][predictions == 1]

            # Extract hours from the timestamps
            stress_hours = stress_times.dt.hour.unique()

            return stress_hours
        else:
            raise ValueError("hr_data must be a DataFrame with 'timestamp' and 'heart rate' columns")


# Example usage
stress_files = ['data/9_5_stressLevel.csv', 'data/8_5_stressLevel.csv', 'data/hjhtkr-stressLevel.csv', 'data/dovevshachar-stressLevel.csv']
hr_files = ['data/9_5_heartRate.csv', 'data/8_5_heartRate.csv', 'data/hjhtkr-heartRate.csv', 'data/dovevshachar-heartRate.csv']

# Instantiate the model builder
model_builder = StressModelBuilder()

# Load and preprocess data
X, y = model_builder.load_and_preprocess_data(stress_files, hr_files)

# Train the model
model_builder.train_model(X, y)

# Predict stress hours from the new heart rate file
test_hr_file = 'data/5_5_heartRate.csv'
stress_hours = model_builder.predict_stress_hours(test_hr_file)
print("Stress Hours:", stress_hours)
















# import pandas as pd
# from sklearn.model_selection import train_test_split
# from sklearn.preprocessing import StandardScaler
# from sklearn.ensemble import RandomForestClassifier
# from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
#
#
# class StressModelBuilder:
#     def __init__(self):
#         self.model = RandomForestClassifier(random_state=42)
#         self.scaler = StandardScaler()
#
#     def load_and_preprocess_data(self, stress_files, hr_files):
#         all_stress_data = []
#         all_hr_data = []
#
#         for stress_file in stress_files:
#             stress_data = pd.read_csv(stress_file)
#             all_stress_data.append(stress_data)
#
#         for hr_file in hr_files:
#             hr_data = pd.read_csv(hr_file)
#             all_hr_data.append(hr_data)
#
#         stress_data = pd.concat(all_stress_data)
#         hr_data = pd.concat(all_hr_data)
#
#         # Keep only relevant columns
#         hr_data_cleaned = hr_data[['timestamp', 'heart rate']].dropna()
#         stress_data_cleaned = stress_data[['stress level value', 'stress level time']]
#
#         # Convert timestamps to datetime for better handling
#         hr_data_cleaned['timestamp'] = pd.to_datetime(hr_data_cleaned['timestamp'])
#         stress_data_cleaned['stress level time'] = pd.to_datetime(stress_data_cleaned['stress level time'])
#
#         # Interpolate missing heart rate values
#         hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], errors='coerce')
#         hr_data['heart rate'] = hr_data['heart rate'].interpolate(method='linear')
#
#         # Drop rows where the 'timestamp' is still NaN after coercion
#         hr_data_cleaned = hr_data.dropna(subset=['timestamp', 'heart rate'])
#
#         # Keep only relevant columns
#         hr_data_cleaned = hr_data_cleaned[['timestamp', 'heart rate']]
#
#         # Ensure that both datasets have timestamps in the same format
#         hr_data_cleaned['timestamp'] = pd.to_datetime(hr_data_cleaned['timestamp'])
#         stress_data_cleaned['stress level time'] = pd.to_datetime(stress_data_cleaned['stress level time'])
#
#         # Drop rows with NaN values in stress level time
#         stress_data_cleaned = stress_data_cleaned.dropna(subset=['stress level time'])
#
#         # Merge the datasets on the closest timestamp
#         merged_data = pd.merge_asof(stress_data_cleaned.sort_values('stress level time'),
#                                     hr_data_cleaned.sort_values('timestamp'),
#                                     left_on='stress level time',
#                                     right_on='timestamp',
#                                     direction='nearest')
#
#         # Create features
#         merged_data['hr_rolling_mean_5'] = merged_data['heart rate'].rolling(window=5).mean().shift(1)
#         merged_data['hr_diff'] = merged_data['heart rate'].diff().shift(1)
#
#         # Drop rows with NaN values created by rolling and diff
#         merged_data.dropna(inplace=True)
#
#         X = merged_data[['heart rate', 'hr_rolling_mean_5', 'hr_diff']]
#         y = merged_data['stress level value'] > 50  # Assuming stress level above 50 indicates stress
#
#         return X, y
#
#     def train_model(self, X, y):
#         # Split the data
#         X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
#
#         # Standardize the features
#         X_train_scaled = self.scaler.fit_transform(X_train)
#         X_test_scaled = self.scaler.transform(X_test)
#
#         # Train the RandomForest model
#         self.model.fit(X_train_scaled, y_train)
#
#         # Evaluate the model
#         y_pred = self.model.predict(X_test_scaled)
#         print("Classification Report:")
#         print(classification_report(y_test, y_pred))
#         print("Confusion Matrix:")
#         print(confusion_matrix(y_test, y_pred))
#         print("Accuracy:", accuracy_score(y_test, y_pred))
#
#     def predict_stress_hours(self, hr_file):
#         # Load heart rate data from file
#         hr_data = pd.read_csv(hr_file)
#
#         # Ensure hr_data is a DataFrame with the correct columns
#         if 'timestamp' in hr_data.columns and 'heart rate' in hr_data.columns:
#             # Convert timestamp to datetime
#             hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], errors='coerce')
#
#             # Create features from hr_data
#             hr_data['hr_rolling_mean_5'] = hr_data['heart rate'].rolling(window=5).mean().shift(1)
#             hr_data['hr_diff'] = hr_data['heart rate'].diff().shift(1)
#             hr_data = hr_data.dropna()
#
#             if len(hr_data) < 1:
#                 raise ValueError("hr_data must have enough data points to create rolling features")
#
#             # Standardize the features
#             X_block_scaled = self.scaler.transform(hr_data[['heart rate', 'hr_rolling_mean_5', 'hr_diff']])
#
#             # Predict stress levels
#             predictions = self.model.predict(X_block_scaled)
#
#             # Get the timestamps where stress is predicted
#             stress_times = hr_data['timestamp'][predictions == 1]
#
#             # Extract hours from the timestamps
#             stress_hours = stress_times.dt.hour.unique()
#
#             return stress_hours
#         else:
#             raise ValueError("hr_data must be a DataFrame with 'timestamp' and 'heart rate' columns")
#
#
# # Example usage
# stress_files = ['data/9_5_stressLevel.csv', 'data/8_5_stressLevel.csv', 'data/hjhtkr-stressLevel.csv', 'data/dovevshachar-stressLevel.csv']
# hr_files = ['data/9_5_heartRate.csv', 'data/8_5_heartRate.csv', 'data/hjhtkr-heartRate.csv', 'data/dovevshachar-heartRate.csv']
#
# # Instantiate the model builder
# model_builder = StressModelBuilder()
#
# # Load and preprocess data
# X, y = model_builder.load_and_preprocess_data(stress_files, hr_files)
#
# # Train the model
# model_builder.train_model(X, y)
#
# # Predict stress hours from the new heart rate file
# test_hr_file = 'data/5_5_heartRate.csv'
# stress_hours = model_builder.predict_stress_hours(test_hr_file)
# print("Stress Hours:", stress_hours)
