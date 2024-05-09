import matplotlib.pyplot as plt
import pandas as pd

# Load and merge data
heart_rate_path = 'heart_rate_data_2.csv'
stress_level_path = 'stress_level_data_2.csv'
heart_rate_data = pd.read_csv(heart_rate_path)
stress_level_data = pd.read_csv(stress_level_path)

heart_rate_data.columns = ['timestamp', 'heart rate']
stress_level_data.columns = ['timestamp', 'stress level']

merged_data = pd.merge(heart_rate_data, stress_level_data, on='timestamp', how='inner')

# Load user-reported stress points
stress_report_path = 'user_stress_report_2.csv'
stress_report_data = pd.read_csv(stress_report_path)
stress_report_data.columns = ['timestamp', 'stress_report']

# Plotting both datasets
plt.figure(figsize=(12, 6))
plt.plot(merged_data['timestamp'], merged_data['heart rate'], label='Heart Rate', linestyle='-')
plt.plot(merged_data['timestamp'], merged_data['stress level'], label='Stress Level', linestyle='--')

# Plot dashed vertical lines for user-reported stress points
user_stress_points = stress_report_data[stress_report_data['stress_report'] == 1]['timestamp']
for point in user_stress_points:
    plt.axvline(x=point, color='g', linestyle='--', linewidth=2)

plt.axvline(x=0, ymin=0, ymax=0, linewidth=2,color='g', linestyle='--', label='User-Reported Stress')

plt.title('Heart Rate and Stress Level Over Time')
plt.xlabel('Timestamp')
plt.ylabel('Values')

# Sampling a subset of timestamps for labeling to avoid clutter
sampled_timestamps = merged_data['timestamp'].iloc[::len(merged_data)//10]
plt.xticks(sampled_timestamps, rotation=45)  # Set x-axis ticks to sampled timestamps


plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()
