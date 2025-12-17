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
from typing import Optional, Literal, Any

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


def burnout_recs_by_score(score: int, confidence: float) -> list[dict[str, Any]]:
    """
    Recos détaillées basées uniquement sur burnout_score (0..100).
    Chaque tranche a un plan multi-aspects.
    """
    def rec(priority, severity, title, tag, why, plan):
        return {
            "priority": priority,
            "severity": severity,  # 1..5
            "title": title,
            "tag": tag,
            "why": why,
            "plan": plan
        }

    cautious = confidence < 0.55
    caution_txt = " (prudence: confiance modèle faible)" if cautious else ""

    # Tranches
    if score < 20:
        return [
            rec(2, 1, "Prévention légère", "planning",
                f"Score {score}/100 → faible impact détecté{caution_txt}.",
                {
                    "now": ["Hydratation + posture correcte.", "Micro-pause 60s (respiration lente)."],
                    "next_30_min": ["Planifier 1 pause courte (2–3 min)."],
                    "during_shift": ["Éviter le multitâche inutile.", "Garder une checklist simple."],
                    "after_shift": ["Sommeil régulier.", "Activité relaxante 10 min."],
                    "avoid": ["Enchaîner sans pause toute la journée."]
                })
        ]

    if score < 30:
        return [
            rec(2, 2, "Stabilité & rythme", "planning",
                f"Score {score}/100 → début de fatigue/stress{caution_txt}.",
                {
                    "now": ["Respiration 2 min (4s/6s).", "Boire un verre d’eau."],
                    "next_30_min": ["Faire une pause 3–5 min loin du stress.", "Réduire les interruptions."],
                    "during_shift": ["1 tâche à la fois.", "Prioriser 3 tâches clés."],
                    "after_shift": ["Décompression 10 min.", "Sommeil suffisant."],
                    "avoid": ["Café en continu pour “tenir”."]
                })
        ]

    if score < 40:
        return [
            rec(1, 2, "Réduction de charge", "mental",
                f"Score {score}/100 → surcharge modérée possible{caution_txt}.",
                {
                    "now": ["Lister les tâches et supprimer le non-essentiel.", "Respiration 2 min."],
                    "next_30_min": ["Pause réelle 5–10 min.", "Demander un mini-renfort si possible."],
                    "during_shift": ["Batcher les tâches similaires.", "Utiliser checklists (éviter oublis)."],
                    "after_shift": ["Activité calme + hydratation.", "Sommeil prioritaire."],
                    "avoid": ["Multitâche + décisions sans notes."]
                })
        ]

    if score < 50:
        return [
            rec(1, 3, "Récupération active", "repos",
                f"Score {score}/100 → fatigue claire{caution_txt}.",
                {
                    "now": ["Pause 5 min (assis).", "Hydratation + collation légère."],
                    "next_30_min": ["Marche 2 min + étirements 2 min.", "Réduire tâches complexes."],
                    "during_shift": ["Alterner tâches lourdes/légères.", "Valider les actions critiques si possible."],
                    "after_shift": ["Déconnexion 15 min.", "Sommeil en priorité."],
                    "avoid": ["Ignorer les signaux (maux de tête, irritabilité)."]
                })
        ]

    if score < 60:
        return [
            rec(1, 3, "Plan anti-épuisement", "mental",
                f"Score {score}/100 → risque modéré{caution_txt}.",
                {
                    "now": ["Stop 2 min, respiration lente.", "Réduire interruptions."],
                    "next_30_min": ["Pause 10 min.", "Mettre une checklist obligatoire sur tâches critiques."],
                    "during_shift": ["Valider décisions importantes avec un collègue.", "Limiter surcharge cognitive."],
                    "after_shift": ["Sommeil + repas léger.", "Éviter écrans tardifs."],
                    "avoid": ["Café tardif + absence de pauses."]
                })
        ]

    if score < 70:
        return [
            rec(0, 4, "Alerte – surcharge importante", "sécurité",
                f"Score {score}/100 → surcharge importante{caution_txt}.",
                {
                    "now": ["Éviter décisions critiques seul.", "Passer en binôme sur actes sensibles."],
                    "next_30_min": ["Pause 10–15 min (réelle).", "Hydratation + respiration."],
                    "during_shift": ["Simplifier: tâches simples + validation croisée.", "Réduire multitâche."],
                    "after_shift": ["Récupération prioritaire.", "Ne pas planifier activités lourdes."],
                    "avoid": ["Continuer “comme si de rien n’était”."]
                }),
            rec(1, 3, "Récupération", "repos",
                f"Score {score}/100 → besoin de récupération{caution_txt}.",
                {
                    "now": ["S’asseoir 3–5 min.", "Étirements (cou/épaules)."],
                    "next_30_min": ["Si possible micro-sieste 10–15 min."],
                    "during_shift": ["Micro-pauses toutes 45–60 min."],
                    "after_shift": ["Sommeil et hydratation."],
                    "avoid": ["Rester debout sans pause longtemps."]
                })
        ]

    if score < 80:
        return [
            rec(0, 4, "Risque élevé – actions immédiates", "sécurité",
                f"Score {score}/100 → risque élevé{caution_txt}.",
                {
                    "now": ["Binôme sur tâches critiques.", "Utiliser checklists systématiques."],
                    "next_30_min": ["Pause 15 min.", "Air frais/lumière vive si possible."],
                    "during_shift": ["Éviter tâches complexes longues.", "Limiter décisions lourdes."],
                    "after_shift": ["Récupération stricte (sommeil).", "Éviter conduite longue si somnolence."],
                    "avoid": ["Prendre des risques / aller vite pour “finir”."]})
        ]

    # 80–100
    return [
        rec(0, 5, "Critique – sécuriser immédiatement", "sécurité",
            f"Score {score}/100 → niveau critique{caution_txt}.",
            {
                "now": ["Stopper tâches à risque si possible.", "Informer un responsable / demander relais."],
                "next_30_min": ["Pause réelle 15–20 min (récupération).", "Hydratation + collation légère."],
                "during_shift": ["Tâches simples uniquement.", "Validation obligatoire des actes critiques."],
                "after_shift": ["Récupération + éviter conduite si somnolence.", "Considérer avis pro si répétitif."],
                "avoid": ["Rester seul en zone critique.", "Continuer sans pause."]
            })
    ]


@app.post("/predict_personalized")
def predict_burnout_personalized(req: BurnoutRequest):
    X = np.array(req.answers).reshape(1, -1)
    pred = int(burnout_model.predict(X)[0])
    proba = burnout_model.predict_proba(X)[0].tolist()
    label = BURNOUT_LABELS[pred]

    burnout_score = int(round(sum(req.answers) / (12 * 4) * 100))
    confidence = float(max(proba)) if proba else 0.0

    # Titre/message basés sur score (pas sur label uniquement)
    if burnout_score < 35:
        risk_title = "Risque Faible"
        message = "Risque faible selon votre score."
    elif burnout_score < 70:
        risk_title = "Risque Modéré"
        message = "Risque modéré selon votre score. Surveillez votre charge et votre récupération."
    else:
        risk_title = "Risque Élevé"
        message = "Risque élevé selon votre score. Réduction de surcharge et récupération recommandées."

    recs = burnout_recs_by_score(burnout_score, confidence)

    return {
        "risk_level": pred,
        "risk_label": label,
        "burnout_score": burnout_score,
        "risk_title": risk_title,
        "message": message,
        "confidence": confidence,
        "probabilities": proba,
        "personalized_recommendations": recs
    }


# ======================================================
# 2) Fatigue Image Model
# ======================================================
BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

FATIGUE_MODEL_PATH = MODELS_DIR / "fatigue_cnn_baseline.keras"

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

    def add(priority, severity, title, tag, why, plan):
        recs.append({
            "priority": priority,        # 0 = urgent
            "severity": severity,        # 1..5
            "title": title,
            "tag": tag,
            "why": why,
            "plan": plan
        })

    # Helpers
    def plan_basic(now=None, next30=None, during=None, after=None, avoid=None):
        return {
            "now": now or [],
            "next_30_min": next30 or [],
            "during_shift": during or [],
            "after_shift": after or [],
            "avoid": avoid or []
        }

    is_low = (risk_label == "Faible") or (fatigue_score < 35)
    is_mod = (risk_label == "Moyen") or (35 <= fatigue_score < 70)
    is_high = (risk_label == "Élevé") or (fatigue_score >= 70)

    # ======================================================
    # ✅ 0) RISQUE FAIBLE (prévention)
    # ======================================================
    if is_low:
        add(
            2, 1,
            "Prévention – maintenir l’énergie",
            "planning",
            "Fatigue faible : l’objectif est de prévenir l’accumulation sur le service.",
            plan_basic(
                now=["Hydratation.", "Relâcher épaules/nuque 30s."],
                next30=["Mini-pause 1–2 minutes.", "Marche 1 minute si possible."],
                during=["Alterner tâches (éviter monotonie).", "Micro-pauses toutes 60–90 min."],
                after=["Sommeil régulier.", "Déconnexion écran 30 min avant sommeil."],
                avoid=["Boire trop de café “par habitude”."]
            )
        )

        if ctx.had_breaks is False:
            add(
                2, 2,
                "Pause manquante",
                "pause",
                "Même avec fatigue faible, l’absence de pause accélère la fatigue.",
                plan_basic(
                    now=["Prendre 3–5 minutes de pause dès que possible."],
                    during=["Planifier 1 pause courte par bloc de 2h."],
                    avoid=["Enchaîner tout le service sans pause."]
                )
            )

        if ctx.shift in ["Nuit", "Garde"]:
            add(
                2, 2,
                "Prévention en garde/nuit",
                "shift",
                "La nuit baisse naturellement la vigilance même si le score est faible.",
                plan_basic(
                    during=["Éviter tâches monotones longues.", "Lumière vive si possible."],
                    avoid=["Se surcharger en fin de garde."]
                )
            )

        recs.sort(key=lambda x: x["priority"])
        return recs[:5]

    # ======================================================
    # ✅ 1) RISQUE MODÉRÉ
    # ======================================================
    if is_mod:
        add(
            1, 3,
            "Récupération active",
            "repos",
            "Fatigue modérée : une récupération courte améliore la vigilance et réduit les erreurs.",
            plan_basic(
                now=["Pause 5 minutes assis.", "Étirements 2 minutes."],
                next30=["Hydratation + collation légère."],
                during=["Micro-pauses toutes 45–60 min.", "Limiter multitâche."],
                after=["Sommeil prioritaire.", "Éviter café tardif."]
            )
        )

        if profile["vigilance"] >= 3 or ctx.shift in ["Nuit", "Garde"]:
            add(
                1, 3,
                "Protéger la vigilance",
                "vigilance",
                "Le risque de baisse de vigilance augmente avec la nuit, le manque de sommeil et la fatigue.",
                plan_basic(
                    now=["Se lever et marcher 2 minutes.", "Lumière vive si possible."],
                    during=["Valider actes importants (double-check).", "Alterner tâches."],
                    avoid=["Rester seul sur tâches critiques en fin de service."]
                )
            )

        if profile["mental"] >= 3:
            add(
                1, 3,
                "Surcharge mentale",
                "mental",
                "Le stress diminue la qualité des décisions et la concentration.",
                plan_basic(
                    now=["Respiration lente 2–3 min (4s/6s)."],
                    during=["Faire 1 tâche à la fois.", "Utiliser checklist simple."],
                    avoid=["Décisions importantes en état de surcharge."]
                )
            )

        if ctx.consecutive_shifts is not None and ctx.consecutive_shifts >= 3:
            add(
                2, 3,
                "Fatigue cumulative",
                "planning",
                "Les gardes consécutives favorisent l’épuisement progressif.",
                plan_basic(
                    after=["Prévoir récupération prolongée après service.", "Réduire activités non essentielles."],
                    avoid=["Prolonger la journée après une série de gardes."]
                )
            )

        recs.sort(key=lambda x: x["priority"])
        return recs[:5]

    # ======================================================
    # ✅ 2) RISQUE ÉLEVÉ (sécurité)
    # ======================================================
    if is_high:
        add(
            0, 5,
            "Alerte sécurité – réduire le risque d’erreur",
            "sécurité",
            "Fatigue élevée : le risque d’erreur et de somnolence augmente fortement.",
            plan_basic(
                now=["Éviter tâches à risque (médication/décisions critiques).", "Travailler en binôme si possible."],
                next30=["Pause réelle 10–15 min.", "Hydratation + collation légère."],
                during=["Tâches simples + validation croisée.", "Limiter multitâche."],
                after=["Éviter conduite si somnolence.", "Sommeil strict en priorité."],
                avoid=["Accélérer pour “finir vite”."]
            )
        )

        if profile["physical"] >= 3 or (ctx.hours_slept is not None and ctx.hours_slept < 6):
            add(
                1, 4,
                "Récupération indispensable",
                "repos",
                "Le manque de sommeil et la fatigue physique diminuent la concentration et les réflexes.",
                plan_basic(
                    now=["Pause 15–20 min si possible.", "Micro-sieste 10–15 min si autorisée."],
                    during=["Alterner tâches.", "Réduire efforts physiques."],
                    after=["Repos prolongé.", "Éviter écrans tard."]
                )
            )

        if profile["vigilance"] >= 3 or ctx.shift in ["Nuit", "Garde"]:
            add(
                1, 4,
                "Somnolence / baisse de vigilance",
                "vigilance",
                "La vigilance est particulièrement fragile en nuit/garde.",
                plan_basic(
                    now=["Lumière vive.", "Marche 2 minutes.", "Hydratation."],
                    during=["Stop décisions critiques en autonomie.", "Double-check systématique."],
                    avoid=["Rester isolé sur tâches critiques."]
                )
            )

        recs.sort(key=lambda x: x["priority"])
        return recs[:5]

    # fallback
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
