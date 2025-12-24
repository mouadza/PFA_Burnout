import { Component, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiClientService } from '../../services/api-client.service';

@Component({
  selector: 'app-questionnaire',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './questionnaire.component.html',
  styleUrl: './questionnaire.component.scss'
})
export class QuestionnaireComponent {
  currentQuestionIndex = 0;
  selectedOption: any = null;
  answers: (number | null)[] = [];
  isLoading = false;
  toastMessage: string = '';
  toastVisible: boolean = false;
  
  questions = [
    "Je me suis senti mentalement épuisé durant ce service.",
    "Je me suis senti physiquement fatigué à cause de la charge de travail.",
    "J'ai eu du mal à gérer les situations stressantes ou critiques.",
    "Je me suis senti débordé par le nombre de patients ou de tâches.",
    "J'ai eu des difficultés à me concentrer ou à penser clairement.",
    "J'ai commis ou failli commettre une erreur à cause de la fatigue.",
    "Le rythme de travail était trop rapide pour être géré sereinement.",
    "Je n'ai pas eu assez de temps pour accomplir correctement mes tâches.",
    "Je me suis senti émotionnellement détaché des patients.",
    "Je me suis senti moins motivé ou impliqué que d'habitude.",
    "Je me suis senti frustré ou insatisfait de ma performance.",
    "Ce service a eu un impact négatif sur mon bien-être général."
  ];

  likertLabels = [
    { text: "Jamais", value: 0 },
    { text: "Rarement", value: 1 },
    { text: "Parfois", value: 2 },
    { text: "Souvent", value: 3 },
    { text: "Toujours", value: 4 }
  ];

  constructor(
    private router: Router,
    private apiClient: ApiClientService,
    private cdr: ChangeDetectorRef
  ) {
    this.answers = new Array(this.questions.length).fill(null);
  }

  get currentQuestion() {
    return this.questions[this.currentQuestionIndex];
  }

  getProgressPercentage(): number {
    return ((this.currentQuestionIndex + 1) / this.questions.length) * 100;
  }

  selectOption(option: any) {
    this.selectedOption = option;
    this.answers[this.currentQuestionIndex] = option.value;
  }

  async nextQuestion() {
    // Vérifier la limite 24h AVANT de passer à la question suivante
    const isLimited = await this.check24HourLimit();
    if (isLimited) {
      // Si la limite est atteinte, ne pas avancer et rester sur la question actuelle
      return;
    }
    
    // ✅ on autorise de passer même sans choix
    if (this.currentQuestionIndex < this.questions.length - 1) {
      this.currentQuestionIndex++;
      this.selectedOption = this.likertLabels.find(
        opt => opt.value === this.answers[this.currentQuestionIndex]
      ) || null;
      this.cdr.detectChanges();
      return;
    }

    // Dernière question => tentative d'envoi
    this.submitQuestionnaire();
  }

  private async check24HourLimit(): Promise<boolean> {
    try {
      const token = this.apiClient.getToken();
      if (token) {
        const lastResults = await this.apiClient.get<any[]>('/burnout-results/me');
        if (lastResults && lastResults.length > 0) {
          const lastResult = lastResults[0];
          if (lastResult.createdAt) {
            const lastDate = new Date(lastResult.createdAt);
            const now = new Date();
            const hoursSinceLastTest = (now.getTime() - lastDate.getTime()) / (1000 * 60 * 60);
            
            if (hoursSinceLastTest < 24) {
              const message = 'Vous ne pouvez effectuer qu\'un seul test par 24 heures. Veuillez réessayer plus tard.';
              this.showToast(message);
              this.cdr.detectChanges();
              return true; // Limite atteinte - empêcher l'avancement
            }
          }
        }
      }
    } catch (error: any) {
      console.error('Error checking last test:', error);
    }
    return false; // Pas de limite - permettre l'avancement
  }

private showToast(message: string) {
  this.toastMessage = message;
  this.toastVisible = true;
  this.cdr.detectChanges();
  
  // Disparaît après 5 secondes
  setTimeout(() => {
    this.toastVisible = false;
    this.toastMessage = '';
    this.cdr.detectChanges();
  }, 5000);
}

async submitQuestionnaire() {
  // ✅ vérifier les questions non répondues
  const missingIndexes = this.answers
    .map((a, i) => (a === null ? i + 1 : null))
    .filter(v => v !== null) as number[];

  if (missingIndexes.length > 0) {
    const message = `Vous n'avez pas répondu à toutes les questions. Questions manquantes : ${missingIndexes.join(', ')}. Merci de compléter avant d'envoyer.`;
    
    // Revenir à la première question manquante AVANT d'afficher le toast
    this.currentQuestionIndex = (missingIndexes[0] - 1);
    this.selectedOption = this.likertLabels.find(
      opt => opt.value === this.answers[this.currentQuestionIndex]
    ) || null;
    
    // Afficher le toast après avoir changé la question
    this.showToast(message);
    this.cdr.detectChanges();
    return;
  }

  // Vérifier si l'utilisateur a déjà soumis un test dans les 24 dernières heures
  const isLimited = await this.check24HourLimit();
  if (isLimited) {
    // Si la limite est atteinte, ne pas soumettre le questionnaire
    return;
  }

  this.isLoading = true;
  this.cdr.detectChanges();

  try {
    // 1. Obtenir la prédiction depuis FastAPI
    const response = await this.apiClient.postToFastApi<any>(
      '/predict_personalized',
      { answers: this.answers.map(a => a ?? 0) }
    );

    const resultData = {
      burnoutScore: Math.round(response.burnout_score || 0),
      riskLabel: response.risk_label || 'Moyen',
      riskTitle: response.risk_title || 'Résultat',
      message: response.message || '',
      recommendation: Array.isArray(response.personalized_recommendations)
        ? response.personalized_recommendations.map((r: any) => r.title || r).join(', ')
        : '',
      answers: this.answers.map(a => a ?? 0)
    };

    // 2. Sauvegarder le résultat dans le backend
    try {
      const token = this.apiClient.getToken();
      if (token) {
        await this.apiClient.post('/burnout-results', resultData);
        console.log('[Questionnaire] Result saved successfully');
        
        // Afficher un message de confirmation
        this.showToast('Résultat sauvegardé avec succès !');
        this.cdr.detectChanges();
        
        // Attendre un peu pour que l'utilisateur voie le message
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
    } catch (saveError: any) {
      console.error('[Questionnaire] Error saving result:', saveError);
      // Continuer quand même pour afficher le résultat, même si la sauvegarde échoue
    }

    // 3. Préparer les données pour la navigation
    const navigationData = {
      score: resultData.burnoutScore,
      riskTitle: resultData.riskTitle,
      riskLabel: resultData.riskLabel,
      message: resultData.message,
      confidence: response.confidence || 0,
      recommendations: Array.isArray(response.personalized_recommendations)
        ? response.personalized_recommendations
        : [],
      answers: resultData.answers
    };

    // 4. Rediriger vers la page de résultat
    this.router.navigate(['/questionnaire-result'], { state: navigationData });

  } catch (error: any) {
    console.error('Error submitting questionnaire:', error);
    this.isLoading = false;
    this.cdr.detectChanges();
    
    const errorMessage = error.message || 'Erreur de connexion';
    
    // Vérifier si c'est une erreur de limitation 24h
    if (errorMessage.toLowerCase().includes('24') || errorMessage.toLowerCase().includes('heure')) {
      this.showToast('Vous ne pouvez effectuer qu\'un seul test par 24 heures. Veuillez réessayer plus tard.');
    } else {
      this.showToast(`Erreur de connexion : ${errorMessage}`);
    }
  }
}


  previousQuestion() {
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
      this.selectedOption = this.likertLabels.find(
        opt => opt.value === this.answers[this.currentQuestionIndex]
      ) || null;
    }
  }
}

