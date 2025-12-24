import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute, NavigationExtras } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiClientService } from '../../services/api-client.service';

@Component({
  selector: 'app-questionnaire-result',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './questionnaire-result.component.html',
  styleUrl: './questionnaire-result.component.scss'
})
export class QuestionnaireResultComponent implements OnInit {
  score: number = 0;
  riskTitle: string = '';
  riskLabel: string = '';
  message: string = '';
  confidence: number = 0;
  recommendations: any[] = [];
  answers: number[] = [];
  saving = false;
  saveError: string | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private apiClient: ApiClientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Try to get data from navigation state first
    const navigation = this.router.getCurrentNavigation();
    let state: any = null;
    
    if (navigation?.extras?.state) {
      state = navigation.extras.state;
    } else {
      // Fallback: try to get from history state (browser history)
      const historyState = (window.history.state || {}) as any;
      if (historyState && historyState.score !== undefined) {
        state = historyState;
      }
    }
    
    if (state) {
      console.log('[QuestionnaireResult] Loading data from state:', state);
      this.score = state.score || 0;
      this.riskTitle = state.riskTitle || 'Résultat';
      this.riskLabel = state.riskLabel || 'Moyen';
      this.message = state.message || '';
      this.confidence = state.confidence || 0;
      this.recommendations = Array.isArray(state.recommendations) ? state.recommendations : [];
      this.answers = Array.isArray(state.answers) ? state.answers : [];
      
      console.log('[QuestionnaireResult] Data loaded:', {
        score: this.score,
        riskTitle: this.riskTitle,
        riskLabel: this.riskLabel,
        recommendationsCount: this.recommendations.length
      });
      
      this.cdr.detectChanges();
      
      // Le résultat est déjà sauvegardé dans questionnaire.component.ts avant la navigation
      // On ne sauvegarde plus ici pour éviter la duplication
    } else {
      console.warn('[QuestionnaireResult] No state data found, redirecting to questionnaire');
      // If no state, redirect back
      this.router.navigate(['/questionnaire']);
    }
  }

  getColorForRisk(): string {
    switch (this.riskLabel) {
      case 'Faible':
        return '#27ae60';
      case 'Moyen':
        return '#f39c12';
      case 'Élevé':
      default:
        return '#e74c3c';
    }
  }

  getIconForTag(tag: string): string {
    const iconMap: Record<string, string> = {
      'sécurité': 'warning',
      'repos': 'hotel',
      'mental': 'psychology',
      'vigilance': 'visibility',
      'pause': 'pause_circle',
      'shift': 'nightlight_round',
      'planning': 'calendar_month'
    };
    return iconMap[tag] || 'lightbulb_outline';
  }

  getListFrom(value: any): string[] {
    if (Array.isArray(value)) {
      return value.map(e => e.toString());
    }
    return [];
  }

  async saveResult() {
    const token = this.apiClient.getToken();
    if (!token) {
      console.warn('[QuestionnaireResult] No token available, result not saved');
      this.saveError = 'Utilisateur non authentifié.';
      this.cdr.detectChanges();
      return;
    }

    this.saving = true;
    this.saveError = null;
    this.cdr.detectChanges();

    try {
      console.log('[QuestionnaireResult] Saving result to backend...');
      await this.apiClient.post('/burnout-results', {
        burnoutScore: Math.round(this.score),
        riskLabel: this.riskLabel,
        riskTitle: this.riskTitle,
        message: this.message,
        recommendation: Array.isArray(this.recommendations) 
          ? this.recommendations.map((r: any) => r.title || r).join(', ')
          : (this.recommendations || ''),
        answers: this.answers
      });
      console.log('[QuestionnaireResult] Result saved successfully');
    } catch (error: any) {
      console.error('[QuestionnaireResult] Error saving result:', error);
      this.saveError = error.message || "Erreur lors de l'enregistrement du résultat.";
    } finally {
      this.saving = false;
      this.cdr.detectChanges();
    }
  }

  retakeTest() {
    this.router.navigate(['/questionnaire']);
  }

  goHome() {
    this.router.navigate(['/user-home']);
  }
}

