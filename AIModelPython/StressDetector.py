from datetime import datetime

import pandas as pd
import pytz
from sklearn.ensemble import IsolationForest

class StressDetector:
    def __init__(self):
        self.model = None

    def extract_location_features(self, location_data):

        # Convert timestamp columns to datetime
        location_data['starttimestamp'] = pd.to_datetime(location_data['activitysegment_duration_starttimestamp'])
        location_data['endtimestamp'] = pd.to_datetime(location_data['activitysegment_duration_endtimestamp'])

        # 1. Different locations during the day
        unique_locations_count = location_data[['activitysegment_startlocation_latitudee7',
                                                'activitysegment_startlocation_longitudee7']].drop_duplicates().shape[0]

        # 2. Time of driving during the day
        driving_segments = location_data[location_data['activitysegment_activitytype'] == 'IN_PASSENGER_VEHICLE']
        driving_segments['driving_duration'] = (
                    driving_segments['endtimestamp'] - driving_segments['starttimestamp']).dt.total_seconds()
        total_driving_time = driving_segments['driving_duration'].sum()

        # # 3. Time at home (using the most frequent location as home)
        # most_frequent_location_final = location_data[['activitysegment_startlocation_latitudee7',
        #                                               'activitysegment_startlocation_longitudee7']].mode().iloc[0]
        #
        # # Filter rows where the location matches the identified home location either at start or end
        # home_segments_start_final = location_data[(location_data['activitysegment_startlocation_latitudee7'] ==
        #                                            most_frequent_location_final[
        #                                                'activitysegment_startlocation_latitudee7']) &
        #                                           (location_data['activitysegment_startlocation_longitudee7'] ==
        #                                            most_frequent_location_final[
        #                                                'activitysegment_startlocation_longitudee7'])]
        #
        # home_segments_end_final = location_data[(location_data['activitysegment_endlocation_latitudee7'] ==
        #                                          most_frequent_location_final[
        #                                              'activitysegment_startlocation_latitudee7']) &
        #                                         (location_data['activitysegment_endlocation_longitudee7'] ==
        #                                          most_frequent_location_final[
        #                                              'activitysegment_startlocation_longitudee7'])]
        #
        # # Combine both sets of segments
        # home_segments_combined_final = pd.concat([home_segments_start_final, home_segments_end_final]).drop_duplicates()
        #
        # # Calculate time spent at home
        # home_segments_combined_final['time_at_home'] = (
        #             home_segments_combined_final['endtimestamp'] - home_segments_combined_final[
        #         'starttimestamp']).dt.total_seconds()
        # total_time_at_home_final = home_segments_combined_final['time_at_home'].sum()

        return {
            "unique_locations_count": unique_locations_count,
            "total_driving_time_seconds": total_driving_time
            # ,"total_time_at_home_seconds": total_time_at_home_final
        }

    def fill_heart_rate_intervals(self, hr_data):

        hr_data['timestamp'] = hr_data['timestamp'].fillna(method='ffill')
        hr_data['timestamp_16'] = hr_data['timestamp_16'].fillna(0)

        # Convert the original timestamp to datetime
        hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'], format='%Y-%m-%dT%H:%M:%S.%fZ')

        def convert_garmin_timestamp(row, timezone='Asia/Jerusalem'):
            # Garmin epoch offset (seconds since 1970-01-01 00:00:00 UTC)
            GARMIN_EPOCH_OFFSET = 631065600

            # Extract the timestamp from the row
            timestamp_16 = int(row['timestamp_16'])
            timestamp_value = row['timestamp']


            # Convert the provided pandas timestamp to a Unix timestamp (seconds since 1970-01-01 00:00:00 UTC)
            unix_timestamp = int(timestamp_value.timestamp())

            if timestamp_16 == 0:
                return datetime.fromtimestamp(unix_timestamp, pytz.timezone(timezone))
            # Calculate initial timestamp adjusted for Garmin epoch
            timestamp = unix_timestamp - GARMIN_EPOCH_OFFSET

            # Calculate mesgTimestamp by combining adjusted timestamp with timestamp_16
            mesgTimestamp = (timestamp & 0xffff0000) | timestamp_16
            if mesgTimestamp < timestamp:
                mesgTimestamp += 0x10000

            # Apply the Garmin epoch offset again for the final timestamp
            final_timestamp = mesgTimestamp + GARMIN_EPOCH_OFFSET

            # Convert the final timestamp to datetime with the specified timezone
            final_datetime = datetime.fromtimestamp(final_timestamp, pytz.timezone(timezone))

            return final_datetime


        hr_data['timestamp'] = hr_data.apply(convert_garmin_timestamp, axis=1)

        # Select only the necessary columns
        hr_data = hr_data[['timestamp', 'heart_rate']]
        time_range = pd.date_range(start=hr_data['timestamp'].min(), end=hr_data['timestamp'].max(), freq='T')
        resampled_data = pd.DataFrame(index=time_range)
        hr_data = pd.merge(resampled_data, hr_data, left_index=True, right_on='timestamp', how='outer')
        hr_data['heart_rate'] = hr_data['heart_rate'].interpolate(method='linear')
        hr_data.dropna(subset=['heart_rate'], inplace=True)
        return hr_data

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

    def convertToIsraelTimeZone(self, stress_data):
        # Ensure 'stress_level_time' is in datetime format
        stress_data['stress_level_time'] = pd.to_datetime(stress_data['stress_level_time'])

        # Check if the timestamps are timezone-aware
        if stress_data['stress_level_time'].dt.tz is None:
            # Localize to UTC if they are naive
            stress_data['stress_level_time'] = stress_data['stress_level_time'].dt.tz_localize('UTC')

        # Convert to Israel Standard Time (IST)
        israel_tz = pytz.timezone('Asia/Jerusalem')
        stress_data['stress_level_time'] = stress_data['stress_level_time'].dt.tz_convert(israel_tz)

    #
    def extract_sleep_data_for_date(self, sleep_data, date):
        return sleep_data[sleep_data['calendardate'] == date]

    def extract_hr_features(self, hr_data):
        hr_data['timestamp'] = pd.to_datetime(hr_data['timestamp'])
        return hr_data[['timestamp', 'heart_rate']]

    def extract_stress_features(self, stress_data):
        stress_data['stress_level_time'] = pd.to_datetime(stress_data['stress_level_time'])
        self.convertToIsraelTimeZone(stress_data)
        return stress_data[['stress_level_time', 'stress_level_value']]


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
    ['data/idan_stressLevel_18_04.csv', 'data/idan_stressLevel_01_05.csv'],
    ['data/idan_hr_18_04.csv', 'data/idan_hr_01_05.csv'],
    ['data/filtered_locations_2024_04_18.csv', 'data/filtered_locations_2024_05_01.csv'],
    ['data/filtered_sleep_data_2024_04_18.csv', 'data/filtered_sleep_data_2024_05_01.csv']
)
stress_times = detector.predict_stress('data/idan_stressLevel_26_04.csv', 'data/idan_hr_26_04.csv', 'data/filtered_locations_2024_04_26.csv', 'data/filtered_sleep_data_2024_04_26.csv')
print(stress_times)
