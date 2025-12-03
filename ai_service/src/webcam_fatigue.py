import cv2
import numpy as np
import tensorflow as tf
from tensorflow import keras

# ==== CONFIG ====
MODEL_PATH = "../models/fatigue_cnn_qiyu_best.keras"  # adapte si nom différent
IMG_HEIGHT = 224
IMG_WIDTH = 224

# Correspond à l'ordre de sortie de ton modèle
CLASS_NAMES = ["alert", "non_vigilant", "tired"]

# ==== LOAD MODEL ====
print("[INFO] Loading model...")
model = keras.models.load_model(MODEL_PATH)
print("[INFO] Model loaded.")

# ==== LOAD FACE DETECTOR ====
face_cascade = cv2.CascadeClassifier(
    cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
)

if face_cascade.empty():
    print("[ERROR] Cannot load Haar Cascade for face detection")
    exit()

# ==== OPEN WEBCAM ====
cap = cv2.VideoCapture(1)  

if not cap.isOpened():
    print("[ERROR] Cannot open webcam")
    exit()

print("[INFO] Press 'q' to quit")

while True:
    ret, frame = cap.read()
    if not ret:
        print("[ERROR] Failed to grab frame")
        break

    # Option: réduire la taille pour afficher plus vite
    display_frame = cv2.resize(frame, (640, 480))

    # ==== FACE DETECTION ====
    gray = cv2.cvtColor(display_frame, cv2.COLOR_BGR2GRAY)

    # détecter les visages
    faces = face_cascade.detectMultiScale(
        gray,
        scaleFactor=1.3,
        minNeighbors=5,
        minSize=(60, 60)
    )

    if len(faces) > 0:
        # prendre le plus grand visage (celui le plus proche de la caméra)
        x, y, w, h = max(faces, key=lambda box: box[2] * box[3])

        # dessiner un carré sur la tête / visage détecté
        cv2.rectangle(display_frame, (x, y), (x + w, y + h), (0, 255, 0), 2)

        # ---- PREPROCESS: ROI VISAGE ----
        face_roi = display_frame[y:y + h, x:x + w]

        # sécurité si le ROI est vide
        if face_roi.size != 0:
            # BGR -> RGB
            img_rgb = cv2.cvtColor(face_roi, cv2.COLOR_BGR2RGB)
            # resize au format du modèle
            img_resized = cv2.resize(img_rgb, (IMG_WIDTH, IMG_HEIGHT))
            # normalisation
            img_norm = img_resized.astype("float32") / 255.0
            # batch dimension
            input_tensor = np.expand_dims(img_norm, axis=0)

            # ---- PREDICTION ----
            preds = model.predict(input_tensor, verbose=0)[0]
            class_index = int(np.argmax(preds))
            confidence = float(preds[class_index])

            label = CLASS_NAMES[class_index]
            text = f"{label} ({confidence*100:.1f}%)"

            # zone label au-dessus du visage
            cv2.rectangle(display_frame,
                          (x, y - 35),
                          (x + 260, y),
                          (0, 0, 0),
                          -1)
            cv2.putText(display_frame,
                        text,
                        (x + 5, y - 10),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        (0, 255, 0),
                        2,
                        cv2.LINE_AA)
    else:
        # si aucun visage : afficher info
        cv2.rectangle(display_frame, (10, 10), (280, 60), (0, 0, 0), -1)
        cv2.putText(display_frame,
                    "No face detected",
                    (20, 45),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 0, 255),
                    2,
                    cv2.LINE_AA)

    # ==== SHOW ====
    cv2.imshow("Fatigue Detection - Live", display_frame)

    # Quitter avec 'q'
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()