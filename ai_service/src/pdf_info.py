
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import letter
from reportlab.lib.units import inch

# Create the PDF
pdf_path = "../Methode_IA_Burnout_Modern.pdf"
styles = getSampleStyleSheet()

# Modern professional custom styles
title_style = ParagraphStyle(
    'TitleStyle',
    parent=styles['Title'],
    fontName='Helvetica-Bold',
    fontSize=22,
    leading=26,
    spaceAfter=20
)

section_style = ParagraphStyle(
    'SectionStyle',
    parent=styles['Heading2'],
    fontName='Helvetica-Bold',
    fontSize=16,
    leading=20,
    spaceBefore=18,
    spaceAfter=10
)

text_style = ParagraphStyle(
    'TextStyle',
    parent=styles['BodyText'],
    fontName='Helvetica',
    fontSize=11,
    leading=14,
    spaceAfter=12
)

subtitle_style = ParagraphStyle(
    'SubtitleStyle',
    parent=styles['Heading3'],
    fontName='Helvetica-Bold',
    fontSize=13,
    leading=16,
    spaceBefore=10,
    spaceAfter=6
)

content = []

# Title
content.append(Paragraph("Méthodologie d’Apprentissage Automatique", title_style))
content.append(Spacer(1, 0.2*inch))

# Sections
sections = [
    ("1. Objectif", 
    """L’objectif du modèle d’apprentissage automatique est de prédire le niveau de risque de burnout (faible, moyen, élevé) chez les professionnels de santé à partir d’un questionnaire de 12 items administré en fin de service. 
    Les réponses sont cotées sur une échelle de Likert de 0 à 4, permettant une quantification stable et interprétable des symptômes liés à l’épuisement professionnel."""),
    
    ("2. Préparation et structuration des données", 
    """Un jeu de données réaliste a été généré afin de simuler les réponses d’infirmiers et de médecins. Chaque observation contient 12 questions (q1 à q12) représentant divers aspects du burnout, un score global de burnout (0–100) et un niveau de risque catégorisé en trois classes (faible, moyen, élevé).
    La distribution des classes est légèrement déséquilibrée : Faible ~31 %, Moyen ~45 %, Élevé ~24 %."""),

    ("3. Séparation du dataset",
    """Une séparation 80/20 a été appliquée pour l’entraînement et le test. La séparation a été stratifiée afin de conserver les proportions des classes dans les deux sous-ensembles."""),
    
    ("4. Construction du pipeline de traitement",
    """Un pipeline scikit-learn a été mis en place pour assurer reproductibilité et absence de fuites de données. 
    Il inclut : Standardisation des données (StandardScaler) et sélection du classifieur (plusieurs modèles testés)."""),
    
    ("5. Recherche du meilleur modèle : GridSearchCV",
    """Une recherche exhaustive d’hyperparamètres a été effectuée via GridSearchCV avec validation croisée (5 folds). 
    Modèles évalués : Régression Logistique, SVM, Random Forest (XGBoost et LightGBM testés précédemment). 
    66 combinaisons ont été explorées."""),
    
    ("6. Meilleur modèle obtenu",
    """Le meilleur modèle est la Régression Logistique multinomiale avec un score moyen CV de 0.84. 
    Ce choix est cohérent avec la faible dimensionnalité des données, les relations quasi linéaires et le besoin d’interprétabilité en contexte médical."""),
    
    ("7. Évaluation finale sur le jeu de test",
    """Le modèle final obtient une accuracy de 0.81.
    Résultats du rapport de classification :
    • Classe 0 (faible) : précision 0.86, rappel 0.79, F1 = 0.82  
    • Classe 1 (moyen) : précision 0.75, rappel 0.85, F1 = 0.80  
    • Classe 2 (élevé) : précision 0.88, rappel 0.76, F1 = 0.81  
    Le modèle généralise bien et détecte correctement les risques moyens et élevés."""),
    
    ("8. Interprétabilité du modèle",
    """Les coefficients de la régression montrent les items les plus prédictifs : 
    q12 (impact du shift), q10 (baisse de motivation), q9 (détachement émotionnel), q11 (frustration). 
    Ces dimensions correspondent aux axes du burnout de Maslach."""),
    
    ("9. Sauvegarde et déploiement du modèle",
    """Le modèle est sauvegardé via joblib.dump() et peut être intégré dans une API (FastAPI/Flask), une application mobile, un tableau de bord Streamlit ou un système interne hospitalier."""),
    
    ("10. Conclusion",
    """La méthodologie couvre tout le processus (prétraitement → sélection du modèle → évaluation), produit un modèle performant, transparent et adapté à un futur déploiement hospitalier.""")
]

for title, text in sections:
    content.append(Paragraph(title, section_style))
    content.append(Paragraph(text, text_style))

# Build PDF
doc = SimpleDocTemplate(pdf_path, pagesize=letter)
doc.build(content)

pdf_path
