from fastapi import FastAPI, File, UploadFile, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pydantic
import joblib
import numpy as np
import tensorflow as tf
from PIL import Image
import io
import json
from pathlib import Path
from typing import Optional, Literal

# ======================================================
# App
# ======================================================
app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ======================================================
# 1) Burnout Questionnaire Model
# ======================================================
burnout_model = joblib.load("models/burnout_best_model.pkl")
BURNOUT_LABELS = {0: "Faible", 1: "Moyen", 2: "Élevé"}


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
def predict_burnout(req: BurnoutRequest):
    X = np.array(req.answers).reshape(1, -1)
    pred = int(burnout_model.predict(X)[0])
    proba = burnout_model.predict_proba(X)[0].tolist()
    label = BURNOUT_LABELS[pred]

    burnout_score = int(round(sum(req.answers) / (12 * 4) * 100))

    return {
        "risk_level": pred,
        "risk_label": label,
        "burnout_score": burnout_score,
        "probabilities": proba,
    }

# ======================================================
# 2) Fatigue Image Model
# ======================================================
FATIGUE_MODEL_PATH = Path("models") / "fatigue_cnn_baseline.keras"
fatigue_model = tf.keras.models.load_model(FATIGUE_MODEL_PATH)

IMG_HEIGHT = 224
IMG_WIDTH = 224
FATIGUE_LABELS = {0: "Faible", 1: "Moyen", 2: "Élevé"}


def preprocess_image(file_bytes: bytes) -> np.ndarray:
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
    img = img.resize((IMG_WIDTH, IMG_HEIGHT))
    arr = np.array(img, dtype=np.float32) / 255.0
    return np.expand_dims(arr, axis=0)

# ======================================================
# 3) User Context (Personalisation)
# ======================================================
class UserContext(BaseModel):
    role: Optional[Literal["Infirmier", "Medecin", "Interne", "Aide-soignant", "Autre"]] = "Autre"
    department: Optional[str] = "General"
    shift: Optional[Literal["Jour", "Nuit", "Garde"]] = "Jour"
    hours_slept: Optional[float] = None
    stress_level: Optional[int] = None
    had_breaks: Optional[bool] = None
    caffeine_cups: Optional[int] = None
    consecutive_shifts: Optional[int] = None

# ======================================================
# 4) Fatigue Profile (Mini-AI logic)
# ======================================================
def compute_fatigue_profile(fatigue_score: int, ctx: UserContext):
    profile = {"physical": 0, "mental": 0, "vigilance": 0}

    if fatigue_score >= 80:
        profile["physical"] += 2
        profile["mental"] += 2
        profile["vigilance"] += 2
    elif fatigue_score >= 50:
        profile["physical"] += 1
        profile["mental"] += 1

    if ctx.hours_slept is not None and ctx.hours_slept < 6:
        profile["physical"] += 2
        profile["vigilance"] += 2

    if ctx.shift in ["Nuit", "Garde"]:
        profile["vigilance"] += 2

    if ctx.stress_level is not None and ctx.stress_level >= 7:
        profile["mental"] += 2

    if ctx.consecutive_shifts is not None and ctx.consecutive_shifts >= 3:
        profile["physical"] += 1
        profile["mental"] += 1

    return profile

# ======================================================
# 5) Personalized Recommendation Engine
# ======================================================
def build_personalized_recs(risk_label: str, fatigue_score: int, ctx: UserContext):
    recs = []
    profile = compute_fatigue_profile(fatigue_score, ctx)

    def add(priority, title, action, why, tag):
        recs.append({
            "priority": priority,
            "title": title,
            "action": action,
            "why": why,
            "tag": tag
        })

    # ======================================================
    # 🚨 1) SÉCURITÉ AVANT TOUT (très réaliste terrain)
    # ======================================================
    if fatigue_score >= 85:
        add(
            0,
            "Alerte sécurité – vigilance critique",
            "Suspendre toute tâche à risque immédiat (médication, décisions critiques). "
            "Travailler en binôme et informer un supérieur si possible.",
            "Un niveau de fatigue très élevé augmente fortement le risque d’erreur humaine.",
            "sécurité"
        )

    # ======================================================
    # 🧍‍♂️ 2) FATIGUE PHYSIQUE
    # ======================================================
    if profile["physical"] >= 3:
        add(
            1,
            "Récupération physique nécessaire",
            "Prendre une pause réelle de 15–20 minutes (s’asseoir, s’étirer, respirer calmement). "
            "Si autorisé, une micro-sieste de 10–15 minutes est idéale.",
            "Les signes de fatigue physique indiquent une baisse de résistance et de concentration.",
            "repos"
        )

    # ======================================================
    # 🧠 3) FATIGUE MENTALE / STRESS
    # ======================================================
    if profile["mental"] >= 3:
        add(
            1,
            "Surcharge mentale détectée",
            "Réduire temporairement la complexité des tâches. "
            "Faire 2–3 minutes de respiration lente (inspiration 4s / expiration 6s).",
            "Le stress et la charge cognitive réduisent la capacité de prise de décision.",
            "mental"
        )

    # ======================================================
    # 👀 4) BAISSE DE VIGILANCE / SOMNOLENCE
    # ======================================================
    if profile["vigilance"] >= 3:
        add(
            1,
            "Risque de baisse de vigilance",
            "S’hydrater, se lever, marcher 2 minutes et s’exposer à une lumière vive. "
            "Éviter de rester immobile trop longtemps.",
            "La somnolence réduit l’attention et le temps de réaction.",
            "vigilance"
        )

    # ======================================================
    # 🌙 5) CONTEXTE TRAVAIL DE NUIT / GARDE
    # ======================================================
    if ctx.shift in ["Nuit", "Garde"]:
        add(
            2,
            "Organisation du travail de nuit",
            "Privilégier les tâches simples en fin de garde. "
            "Reporter si possible les décisions importantes ou les valider avec un collègue.",
            "Le travail nocturne perturbe le rythme biologique et la vigilance.",
            "shift"
        )

    # ======================================================
    # ⏸️ 6) ABSENCE DE PAUSE
    # ======================================================
    if ctx.had_breaks is False:
        add(
            2,
            "Pause insuffisante",
            "Prendre une pause même courte (5 minutes) dès maintenant, "
            "loin de l’écran ou de l’environnement de stress.",
            "L’absence de pause continue entraîne une accumulation rapide de fatigue.",
            "pause"
        )

    # ======================================================
    # 📅 7) FATIGUE CUMULÉE (gardes consécutives)
    # ======================================================
    if ctx.consecutive_shifts is not None and ctx.consecutive_shifts >= 3:
        add(
            2,
            "Fatigue cumulative détectée",
            "Anticiper une récupération prolongée après le service "
            "(sommeil, réduction d’activités non essentielles).",
            "Les gardes consécutives favorisent l’épuisement progressif.",
            "planning"
        )

    # ======================================================
    # 🔽 TRI FINAL
    # ======================================================
    recs.sort(key=lambda x: x["priority"])
    return recs[:5]


# ======================================================
# 6) Fatigue Prediction (simple)
# ======================================================
@app.post("/fatigue/predict")
async def predict_fatigue(file: UploadFile = File(...)):
    file_bytes = await file.read()

    try:
        Image.open(io.BytesIO(file_bytes)).verify()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid image file")

    X = preprocess_image(file_bytes)
    probs = fatigue_model.predict(X)[0].tolist()
    pred = int(np.argmax(probs))
    label = FATIGUE_LABELS[pred]
    fatigue_score = int(round(max(probs) * 100))

    return {
        "risk_level": pred,
        "risk_label": label,
        "fatigue_score": fatigue_score,
        "probabilities": probs,
    }

# ======================================================
# 7) Fatigue Prediction + Personalized Recs
# ======================================================
@app.post("/fatigue/predict_personalized")
async def predict_fatigue_personalized(
    file: UploadFile = File(...),
    context: str = Form("{}")
):
    file_bytes = await file.read()

    try:
        Image.open(io.BytesIO(file_bytes)).verify()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid image file")

    try:
        ctx = UserContext(**json.loads(context))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid context JSON: {e}")

    X = preprocess_image(file_bytes)
    probs = fatigue_model.predict(X)[0].tolist()
    pred = int(np.argmax(probs))
    label = FATIGUE_LABELS[pred]
    fatigue_score = int(round(max(probs) * 100))

    if pred == 0:
        risk_title = "Risque Faible"
        message = "Aucun signe fort de fatigue détecté."
    elif pred == 1:
        risk_title = "Risque Modéré"
        message = "Des signes possibles de fatigue sont détectés."
    else:
        risk_title = "Risque Élevé"
        message = "Des signes importants de fatigue sont détectés."

    fatigue_profile = compute_fatigue_profile(fatigue_score, ctx)
    personalized = build_personalized_recs(label, fatigue_score, ctx)

    return {
        "risk_level": pred,
        "risk_label": label,
        "fatigue_score": fatigue_score,
        "risk_title": risk_title,
        "message": message,
        "fatigue_profile": fatigue_profile,
        "probabilities": probs,
        "personalized_recommendations": personalized
    }
