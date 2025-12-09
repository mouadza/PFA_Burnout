import numpy as np
import pandas as pd
import os

np.random.seed(42)

# -----------------------------------
# CONFIG
# -----------------------------------
N = 1000           # number of samples
N_QUESTIONS = 12
MAX_SCORE_PER_Q = 4
MAX_RAW = N_QUESTIONS * MAX_SCORE_PER_Q  # 48

os.makedirs("dataset", exist_ok=True)

# -----------------------------------
# 1) Latent burnout level (continuous) + true class
# -----------------------------------
# We simulate 3 groups with overlap:
#  - low burnout: mean ~0.2
#  - medium burnout: mean ~0.5
#  - high burnout: mean ~0.8

# Choose base class distribution
base_class = np.random.choice(
    [0, 1, 2],
    size=N,
    p=[0.30, 0.50, 0.20]
)

latent_burnout = np.zeros(N)

for i, c in enumerate(base_class):
    if c == 0:  # low
        latent_burnout[i] = np.random.normal(0.2, 0.10)
    elif c == 1:  # medium
        latent_burnout[i] = np.random.normal(0.5, 0.10)
    else:  # high
        latent_burnout[i] = np.random.normal(0.8, 0.10)

# clip between 0 and 1
latent_burnout = np.clip(latent_burnout, 0.0, 1.0)

# derive an initial risk class from latent_burnout with thresholds
true_risk = np.zeros(N, dtype=int)
true_risk[latent_burnout > 0.35] = 1
true_risk[latent_burnout > 0.65] = 2

# -----------------------------------
# 2) Add label noise (simulate imperfect ground truth)
#    ~10% of samples will have a mislabeled risk_level
# -----------------------------------
noisy_risk = true_risk.copy()
noise_mask = np.random.rand(N) < 0.10  # 10% mislabeled

for i in np.where(noise_mask)[0]:
    if noisy_risk[i] == 0:
        noisy_risk[i] = np.random.choice([0, 1])  # maybe move to medium
    elif noisy_risk[i] == 1:
        noisy_risk[i] = np.random.choice([0, 1, 2])  # could go down or up
    else:  # class 2
        noisy_risk[i] = np.random.choice([1, 2])  # maybe down to medium

# -----------------------------------
# 3) Generate 12 question responses (0–4) from latent_burnout
#    Scale: 0=Never, 1=Rarely, 2=Sometimes, 3=Often, 4=Always
# -----------------------------------
# Each question has slightly different sensitivity to burnout
question_weights = np.linspace(0.8, 1.2, N_QUESTIONS)

q = np.zeros((N, N_QUESTIONS), dtype=int)

for i in range(N):
    b = latent_burnout[i]  # between 0 and 1
    for j in range(N_QUESTIONS):
        # expected intensity for this question (0–4)
        expected = 4 * b * question_weights[j]
        expected = np.clip(expected, 0, 4)

        # add noise: simulate human variability
        ans = np.random.normal(loc=expected, scale=0.8)

        # round to nearest integer and clamp 0–4
        ans_int = int(round(ans))
        ans_int = max(0, min(4, ans_int))

        q[i, j] = ans_int

# -----------------------------------
# 4) Compute burnout score from answers (0–100)
#    IMPORTANT: This is NOT used to define the label
# -----------------------------------
raw_score = q.sum(axis=1)                     # 0–48
burnout_score = (raw_score / MAX_RAW) * 100  # 0–100

# -----------------------------------
# 5) Final target = noisy_risk (realistic labels)
# -----------------------------------
risk_level = noisy_risk  # this is what you will predict with ML

# -----------------------------------
# 6) Build DataFrame
# -----------------------------------
data = {}
for j in range(N_QUESTIONS):
    data[f"q{j+1}"] = q[:, j]

data["burnout_score"] = burnout_score.round(1)
data["risk_level"] = risk_level          # target label for training
data["true_risk"] = true_risk            # underlying "clean" class
data["latent_burnout"] = latent_burnout  # underlying continuous level

df = pd.DataFrame(data)

# -----------------------------------
# 7) Save
# -----------------------------------
output_path = "dataset/burnout_dataset.csv"
df.to_csv(output_path, index=False)

print(f"Saved: {output_path}")
print(df.head())
print("\nRisk_level distribution:")
print(df['risk_level'].value_counts(normalize=True).round(2))
