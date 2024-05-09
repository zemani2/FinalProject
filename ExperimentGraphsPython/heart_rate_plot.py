import matplotlib.pyplot as plt
import pandas as pd

# Assuming heart_rate_data_2.csv is in the same directory as your script
file_path = 'heart_rate_data_2.csv'  # Adjust this line to the actual relative or absolute path
heart_rate_data = pd.read_csv(file_path)

# Reduce the number of x-axis labels for clarity
plt.figure(figsize=(10, 5))
plt.plot(heart_rate_data['timestamp'], heart_rate_data['heart rate'], linestyle='-')
plt.title('Heart Rate Over Time')
plt.xlabel('Timestamp')
plt.ylabel('Heart Rate (bpm)')

# Sampling a subset of timestamps for labeling to avoid clutter
sampled_timestamps = heart_rate_data['timestamp'].iloc[::len(heart_rate_data)//10]
plt.xticks(sampled_timestamps, rotation=45)
plt.grid(True)
plt.tight_layout()
plt.show()