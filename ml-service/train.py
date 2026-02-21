import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
import joblib
import os

def generate_training_data(n_normal=1000, n_anomalies=50):
    np.random.seed(42)

    # Normal login behavior
    normal_data = {
        'loginFrequency24h': np.random.randint(1, 5, n_normal),
        'isNewIp': np.random.choice([0, 1], n_normal, p=[0.95, 0.05]),
        'isNewDevice': np.random.choice([0, 1], n_normal, p=[0.97, 0.03]),
        'failedAttemptRatio': np.random.uniform(0, 0.1, n_normal),
        'hourOfDay': np.random.randint(8, 22, n_normal),
        'isWeekend': np.random.choice([0, 1], n_normal, p=[0.7, 0.3]),
        'totalUniqueIps': np.random.randint(1, 3, n_normal)
    }

    # Anomalous login behavior
    anomaly_data = {
        'loginFrequency24h': np.random.randint(10, 50, n_anomalies),
        'isNewIp': np.ones(n_anomalies),
        'isNewDevice': np.ones(n_anomalies),
        'failedAttemptRatio': np.random.uniform(0.5, 1.0, n_anomalies),
        'hourOfDay': np.random.randint(0, 5, n_anomalies),
        'isWeekend': np.random.choice([0, 1], n_anomalies),
        'totalUniqueIps': np.random.randint(5, 20, n_anomalies)
    }

    normal_df = pd.DataFrame(normal_data)
    anomaly_df = pd.DataFrame(anomaly_data)

    return pd.concat([normal_df, anomaly_df], ignore_index=True)

def train_model():
    print("Generating training data...")
    df = generate_training_data()

    features = ['loginFrequency24h', 'isNewIp', 'isNewDevice',
                'failedAttemptRatio', 'hourOfDay', 'isWeekend', 'totalUniqueIps']

    X = df[features].values

    print("Scaling features...")
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    print("Training Isolation Forest...")
    model = IsolationForest(
        n_estimators=100,
        contamination=0.05,
        random_state=42,
        max_samples='auto'
    )
    model.fit(X_scaled)

    # Save model and scaler
    os.makedirs('model', exist_ok=True)
    joblib.dump(model, 'model/isolation_forest.pkl')
    joblib.dump(scaler, 'model/scaler.pkl')

    print("Model saved successfully!")

    return {
        'status': 'success',
        'message': 'Model trained successfully',
        'training_samples': len(df),
        'normal_samples': 1000,
        'anomaly_samples': 50,
        'features': features,
        'model': 'IsolationForest',
        'n_estimators': 100,
        'contamination': 0.05
    }

if __name__ == '__main__':
    result = train_model()
    print(result)