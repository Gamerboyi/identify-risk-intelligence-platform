from flask import Flask, request, jsonify
import numpy as np
import joblib
import os
from datetime import datetime

app = Flask(__name__)

# Load model if it exists, otherwise use rule-based scoring
MODEL_PATH = 'model/isolation_forest.pkl'
SCALER_PATH = 'model/scaler.pkl'

model = None
scaler = None

def load_model():
    global model, scaler
    if os.path.exists(MODEL_PATH) and os.path.exists(SCALER_PATH):
        model = joblib.load(MODEL_PATH)
        scaler = joblib.load(SCALER_PATH)
        print("ML model loaded successfully")
    else:
        print("No trained model found - using rule based scoring")

def features_to_array(data):
    return np.array([[
        data.get('loginFrequency24h', 0),
        data.get('isNewIp', 0),
        data.get('isNewDevice', 0),
        data.get('failedAttemptRatio', 0.0),
        data.get('hourOfDay', 12),
        data.get('isWeekend', 0),
        data.get('totalUniqueIps', 1)
    ]])

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'running',
        'model_loaded': model is not None,
        'timestamp': datetime.now().isoformat()
    })

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()

        if not data:
            return jsonify({'error': 'No data provided'}), 400

        features = features_to_array(data)

        if model is not None and scaler is not None:
            # Use trained ML model
            features_scaled = scaler.transform(features)
            score = model.decision_function(features_scaled)[0]

            # Convert isolation forest score to 0-100 risk score
            # decision_function returns negative for anomalies
            # More negative = more anomalous = higher risk
            risk_score = int(max(0, min(100, (-score + 0.5) * 100)))
        else:
            # Fallback to rule based scoring
            risk_score = rule_based_score(data)

        risk_level = get_risk_level(risk_score)

        return jsonify({
            'riskScore': risk_score,
            'riskLevel': risk_level,
            'modelUsed': 'isolation_forest' if model is not None else 'rule_based',
            'features': data
        })

    except Exception as e:
        print(f"Prediction error: {e}")
        return jsonify({
            'riskScore': 40,
            'riskLevel': 'MEDIUM',
            'modelUsed': 'fallback',
            'error': str(e)
        }), 200

@app.route('/train', methods=['POST'])
def train():
    try:
        from train import train_model
        result = train_model()
        load_model()
        return jsonify(result)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def rule_based_score(data):
    score = 0
    if data.get('isNewIp', 0) == 1:
        score += 25
    if data.get('isNewDevice', 0) == 1:
        score += 20
    hour = data.get('hourOfDay', 12)
    if 0 <= hour <= 5:
        score += 20
    if data.get('failedAttemptRatio', 0) > 0.5:
        score += 30
    if data.get('loginFrequency24h', 0) > 10:
        score += 15
    if data.get('totalUniqueIps', 1) > 5:
        score += 10
    return min(score, 100)

def get_risk_level(score):
    if score >= 70:
        return 'HIGH'
    if score >= 30:
        return 'MEDIUM'
    return 'LOW'

if __name__ == '__main__':
    load_model()
    app.run(host='0.0.0.0', port=5000, debug=True)