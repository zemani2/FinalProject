import pandas as pd
from sklearn.ensemble import IsolationForest

class StressDetector:
    def __init__(self):
        self.model = None

    def fill_heart_rate_intervals(self, hr_data):
        """
        Combines the timestamp and timestamp_16 columns, fills in the heart rate values
        at the appropriate minute intervals throughout the day, ensuring no null values
        in the heart rate and timestamp columns.

        Parameters:
        - hr_data (pd.DataFrame): DataFrame containing heart rate data with timestamp and timestamp_16 columns.

        Returns:
        - pd.DataFrame: Processed DataFrame with filled heart rate values and no null timestamps.
        """
        # Convert timestamp columns to datetime
        hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], errors='coerce')
        hr_data['timestamp_16'] = pd.to_datetime(hr_data['timestamp_16'], errors='coerce')

        # Combine the timestamp and timestamp_16 columns
        hr_data['timestamp_combined'] = hr_data['timestamp'].combine_first(hr_data['timestamp_16'])

        # Remove rows where timestamp_combined is still null
        hr_data = hr_data.dropna(subset=['timestamp_combined'])

        # Ensure all timestamps are timezone-naive
        hr_data['timestamp_combined'] = hr_data['timestamp_combined'].dt.tz_localize(None)

        # Create a DataFrame with all minute intervals for the day
        min_timestamp = hr_data['timestamp_combined'].min().floor('D')
        max_timestamp = hr_data['timestamp_combined'].max().ceil('D')
        all_minutes = pd.date_range(start=min_timestamp, end=max_timestamp, freq='T')

        # Merge with the heart rate data to get a DataFrame with all minute intervals
        all_minutes_df = pd.DataFrame(all_minutes, columns=['timestamp_combined'])
        hr_data_filled = pd.merge(all_minutes_df, hr_data[['timestamp_combined', 'heart_rate']],
                                  on='timestamp_combined', how='left')

        # Fill missing heart rate values
        hr_data_filled['heart_rate'] = hr_data_filled['heart_rate'].fillna(method='ffill').fillna(method='bfill')

        return hr_data_filled

    def load_data(self, stress_path, hr_path, location_path, sleep_path):
        # Load the data from the provided files
        stress_data = pd.read_csv(stress_path)
        hr_data = pd.read_csv(hr_path)
        hr_data.columns = [col.replace(' ', '_').replace('.', '_').lower() for col in hr_data.columns]
        hr_data = self.fill_heart_rate_intervals(hr_data)
        location_data = pd.read_csv(location_path)
        sleep_data = pd.read_csv(sleep_path)

        # Convert column names to snake_case
        stress_data.columns = [col.replace(' ', '_').replace('.', '_').lower() for col in stress_data.columns]
        hr_data.columns = [col.replace(' ', '_').replace('.', '_').lower() for col in hr_data.columns]
        location_data.columns = [col.replace(' ', '_').replace('.', '_').lower() for col in location_data.columns]
        sleep_data.columns = [col.replace(' ', '_').replace('.', '_').lower() for col in sleep_data.columns]

        return stress_data, hr_data, location_data, sleep_data
    #
    def extract_sleep_data_for_date(self, sleep_data, date):
        return sleep_data[sleep_data['calendardate'] == date]

    def extract_hr_features(self, hr_data):
        hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'])
        return hr_data[['timestamp', 'heart_rate']]

    def extract_stress_features(self, stress_data):
        stress_data['stress_level_time'] = pd.to_datetime(stress_data['stress_level_time'])
        return stress_data[['stress_level_time', 'stress_level_value']]

    def extract_location_features(self, location_data):
        location_data['activitysegment_duration_starttimestamp'] = pd.to_datetime(location_data['activitysegment_duration_starttimestamp'])
        location_data['activitysegment_duration_endtimestamp'] = pd.to_datetime(location_data['activitysegment_duration_endtimestamp'])
        return location_data[['activitysegment_duration_starttimestamp', 'activitysegment_duration_endtimestamp',
                              'activitysegment_startlocation_latitudee7', 'activitysegment_startlocation_longitudee7']]

    def align_and_merge_data(self, hr_data, stress_data, location_data, sleep_data):
        hr_data = hr_data.dropna(subset=['timestamp', 'heart_rate'])
        stress_data = stress_data.rename(columns={'stress_level_time': 'timestamp'})
        merged_data = pd.merge_asof(stress_data.sort_values('timestamp'), hr_data.sort_values('timestamp'), on='timestamp', direction='nearest')
        sleep_features = sleep_data[['deepsleepseconds', 'lightsleepseconds', 'remsleepseconds', 'awakesleepseconds', 'avgsleepstress']]
        location_data = location_data.rename(columns={'activitysegment_duration_starttimestamp': 'starttimestamp',
                                                      'activitysegment_duration_endtimestamp': 'endtimestamp'})
        return merged_data, sleep_features, location_data

    def combine_features(self, merged_data, sleep_features, location_features):
        sleep_features_repeated = pd.concat([sleep_features] * len(merged_data), ignore_index=True)
        combined_data = pd.concat([merged_data.reset_index(drop=True), sleep_features_repeated], axis=1)

        def is_deviated_location(timestamp):
            for _, location in location_features.iterrows():
                if location['starttimestamp'] <= timestamp <= location['endtimestamp']:
                    return True
            return False

        combined_data['is_deviated_location'] = combined_data['timestamp'].apply(is_deviated_location)
        return combined_data

    def train_model(self, combined_data):
        features = combined_data[['heart_rate', 'deepsleepseconds', 'lightsleepseconds',
                                  'remsleepseconds', 'awakesleepseconds', 'avgsleepstress',
                                  'is_deviated_location']]
        features = features.fillna(features.mean())
        if features.isnull().values.any():
            features = features.fillna(0)
        self.model = IsolationForest(contamination=0.1, random_state=42)
        self.model.fit(features)
        combined_data['anomaly_score'] = self.model.decision_function(features)
        combined_data['anomaly'] = self.model.predict(features)
        return combined_data

    def process_and_train(self, stress_paths, hr_paths, location_paths, sleep_paths):
        all_combined_data = []
        for stress_path, hr_path, location_path, sleep_path in zip(stress_paths, hr_paths, location_paths, sleep_paths):
            stress_data, hr_data, location_data, sleep_data = self.load_data(stress_path, hr_path, location_path, sleep_path)
            date = stress_data['stress_level_time'].iloc[-1][:10]  # Extracting the date from the stress data
            sleep_data_filtered = self.extract_sleep_data_for_date(sleep_data, date)
            hr_features = self.extract_hr_features(hr_data)
            stress_features = self.extract_stress_features(stress_data)
            location_features = self.extract_location_features(location_data)
            merged_data, sleep_features, location_features = self.align_and_merge_data(hr_features, stress_features, location_features, sleep_data_filtered)
            combined_data = self.combine_features(merged_data, sleep_features, location_features)
            all_combined_data.append(combined_data)
        combined_data = pd.concat(all_combined_data, ignore_index=True)
        return self.train_model(combined_data)

    def predict_stress(self, stress_path, hr_path, location_path, sleep_path):
        stress_data, hr_data, location_data, sleep_data = self.load_data(stress_path, hr_path, location_path, sleep_path)
        date = stress_data['stress_level_time'].iloc[-1][:10]
        sleep_data_filtered = self.extract_sleep_data_for_date(sleep_data, date)
        hr_features = self.extract_hr_features(hr_data)
        stress_features = self.extract_stress_features(stress_data)
        location_features = self.extract_location_features(location_data)
        merged_data, sleep_features, location_features = self.align_and_merge_data(hr_features, stress_features, location_features, sleep_data_filtered)
        combined_data = self.combine_features(merged_data, sleep_features, location_features)

        features = combined_data[['heart_rate', 'deepsleepseconds', 'lightsleepseconds',
                                  'remsleepseconds', 'awakesleepseconds', 'avgsleepstress',
                                  'is_deviated_location']]
        features = features.fillna(features.mean())
        if features.isnull().values.any():
            features = features.fillna(0)

        combined_data['anomaly_score'] = self.model.decision_function(features)
        combined_data['anomaly'] = self.model.predict(features)
        stress_times = combined_data[combined_data['anomaly'] == -1]['timestamp']
        return stress_times

# Example usage:
detector = StressDetector()
detector.process_and_train(
    ['data/idan_stressLevel_25_04.csv', 'data/idan_stressLevel_18_04.csv', 'data/idan_stressLevel_01_05.csv'],
    ['data/idan_hr_25_04.csv', 'data/idan_hr_18_04.csv', 'data/idan_hr_01_05.csv'],
    ['data/filtered_locations_2024_04_25.csv', 'data/filtered_locations_2024_04_18.csv', 'data/filtered_locations_2024_05_01.csv'],
    ['data/filtered_sleep_data_2024_04_25.csv', 'data/filtered_sleep_data_2024_04_18.csv', 'data/filtered_sleep_data_2024_05_01.csv']
)
stress_times = detector.predict_stress('data/idan_stressLevel_26_04.csv', 'data/idan_hr_26_04.csv', 'data/filtered_locations_2024_04_26.csv', 'data/filtered_sleep_data_2024_04_26.csv')
print(stress_times)
