from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pydantic
import joblib
import numpy as np

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

model = joblib.load("models/burnout_best_model.pkl")
LABELS = {0: "Faible", 1: "Moyen", 2: "Élevé"}


class BurnoutRequest(BaseModel):
    answers: list[int]

    @pydantic.validator("answers")
    def validate_answers(cls, v):
        if len(v) != 12:
            raise ValueError("answers must contain exactly 12 values")
        if not all(isinstance(x, int) and 0 <= x <= 4 for x in v):
            raise ValueError("each answer must be an int between 0 and 4")
        return v


@app.post("/predict")
def predict(req: BurnoutRequest):
    answers = req.answers
    X = np.array(answers).reshape(1, -1)

    pred = int(model.predict(X)[0])
    proba = model.predict_proba(X)[0].tolist()
    label = LABELS[pred]

    # Score simple 0–100 (optionnel mais utile pour l’UI)
    max_raw = 12 * 4
    burnout_score = int(round(sum(answers) / max_raw * 100))

    if pred == 0:
        risk_title = "Risque Faible"
        message = (
            "Votre niveau de fatigue reste globalement maîtrisé. "
            "Continuez à prendre soin de votre équilibre travail / repos."
        )
        recommendation = (
            "Maintenez vos bonnes habitudes : pauses régulières, sommeil suffisant, "
            "et moments de récupération en dehors de l’hôpital."
        )
    elif pred == 1:
        risk_title = "Risque Modéré"
        message = (
            "Attention, certains signes de fatigue sont présents. "
            "Il est important de mettre en place des mesures préventives dès maintenant."
        )
        recommendation = (
            "Essayez de mieux gérer votre charge de travail, parlez-en à votre encadrement si possible, "
            "et planifiez des temps de récupération (repos, activités relaxantes, soutien social)."
        )
    else:
        risk_title = "Risque Élevé"
        message = (
            "Un niveau de burnout élevé est détecté. "
            "Cette situation peut impacter votre santé et la qualité des soins."
        )
        recommendation = (
            "Il est fortement recommandé de demander du soutien (collègues, encadrement, médecin du travail), "
            "de ralentir le rythme si possible et de consulter un professionnel de santé."
        )

    return {
        "risk_level": pred,
        "risk_label": label,
        "burnout_score": burnout_score,
        "risk_title": risk_title,
        "message": message,
        "recommendation": recommendation,
        "probabilities": proba,
    }
