import matplotlib.pyplot as plt
import pandas as pd

# Load the data
file_path = 'stress_level_data_2.csv'  # Adjust this line if the file is in a different location
stress_level_data = pd.read_csv(file_path)

# Plotting the data
plt.figure(figsize=(10, 5))
plt.plot(stress_level_data['stress level time'], stress_level_data['stress level value'], linestyle='-')
plt.title('Stress Level Over Time')
plt.xlabel('Time')
plt.ylabel('Stress Level')

# Sampling a subset of timestamps for labeling to avoid clutter
sampled_timestamps = stress_level_data['stress level time'].iloc[::len(stress_level_data)//10]
plt.xticks(sampled_timestamps, rotation=45)
plt.grid(True)
plt.tight_layout()
plt.show()