# 1. Image de base
FROM python:3.11-slim

# 2. Dossier de travail
WORKDIR /app

# 3. ⚠️ CORRECTION ICI : Remplacement de libgl1-mesa-glx par libgl1
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# 4. Copie des requirements
COPY requirements.txt .

# 5. Installation des dépendances Python
RUN pip install --no-cache-dir -r requirements.txt

# 6. Copie du code
COPY . .

# 7. Port
EXPOSE 8000

# 8. Lancement
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
