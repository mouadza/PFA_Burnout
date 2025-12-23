import { Component } from '@angular/core';
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
    private apiClient: ApiClientService
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

  nextQuestion() {
  // ✅ on autorise de passer même sans choix
  if (this.currentQuestionIndex < this.questions.length - 1) {
    this.currentQuestionIndex++;
    this.selectedOption = this.likertLabels.find(
      opt => opt.value === this.answers[this.currentQuestionIndex]
    ) || null;
    return;
  }

  // Dernière question => tentative d'envoi
  this.submitQuestionnaire();
}

async submitQuestionnaire() {
  // ✅ vérifier les questions non répondues
  const missingIndexes = this.answers
    .map((a, i) => (a === null ? i + 1 : null))
    .filter(v => v !== null) as number[];

  if (missingIndexes.length > 0) {
    alert(
      `Vous n'avez pas répondu à toutes les questions.\n` +
      `Questions manquantes : ${missingIndexes.join(', ')}.\n` +
      `Merci de compléter avant d'envoyer.`
    );
    // optionnel: revenir à la première question manquante
    this.currentQuestionIndex = (missingIndexes[0] - 1);
    this.selectedOption = this.likertLabels.find(
      opt => opt.value === this.answers[this.currentQuestionIndex]
    ) || null;
    return;
  }

  this.isLoading = true;

  try {
    const response = await this.apiClient.postToFastApi<any>(
      '/predict_personalized',
      { answers: this.answers.map(a => a ?? 0) }
    );

    const navigationData = {
      score: response.burnout_score || 0,
      riskTitle: response.risk_title || 'Résultat',
      riskLabel: response.risk_label || 'Moyen',
      message: response.message || '',
      confidence: response.confidence || 0,
      recommendations: Array.isArray(response.personalized_recommendations)
        ? response.personalized_recommendations
        : [],
      answers: this.answers.map(a => a ?? 0)
    };

    this.router.navigate(['/questionnaire-result'], { state: navigationData });

  } catch (error: any) {
    console.error('Error submitting questionnaire:', error);
    alert(`Erreur de connexion : ${error.message || error}`);
  } finally {
    this.isLoading = false;
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

