import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiClientService } from '../../services/api-client.service';

@Component({
  selector: 'app-fatigue-result',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fatigue-result.component.html',
  styleUrl: './fatigue-result.component.scss'
})
export class FatigueResultComponent implements OnInit {
  score: number = 0;
  confidence: number = 0;
  riskTitle: string = '';
  riskLabel: string = '';
  message: string = '';
  recommendations: any[] = [];
  imageDataUrl: string = '';
  saving = false;
  saveError: string | null = null;

  constructor(
    private router: Router,
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
      console.log('[FatigueResult] Loading data from state:', state);
      this.score = state.score || 0;
      this.confidence = state.confidence || 0;
      this.riskTitle = state.riskTitle || 'Résultat';
      this.riskLabel = state.riskLabel || 'Moyen';
      this.message = state.message || '';
      this.recommendations = this.sanitizeRecommendations(state.recommendations || []);
      this.imageDataUrl = state.imageDataUrl || '';
      
      console.log('[FatigueResult] Data loaded:', {
        score: this.score,
        riskTitle: this.riskTitle,
        riskLabel: this.riskLabel,
        recommendationsCount: this.recommendations.length
      });
      
      this.cdr.detectChanges();
      
      // Auto-save result
      this.saveFatigueResult();
    } else {
      console.warn('[FatigueResult] No state data found, redirecting to fatigue-camera');
      // If no state, redirect back
      this.router.navigate(['/fatigue-camera']);
    }
  }

  sanitizeRecommendations(input: any[]): any[] {
    return input
      .filter(item => item != null)
      .map(item => {
        if (typeof item === 'object' && item !== null) {
          return item;
        }
        return null;
      })
      .filter(item => item !== null);
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

  async saveFatigueResult() {
    const token = this.apiClient.getToken();
    if (!token) {
      console.warn('[FatigueResult] No token available, fatigue result not saved');
      this.saveError = 'Utilisateur non authentifié.';
      this.cdr.detectChanges();
      return;
    }

    this.saving = true;
    this.saveError = null;
    this.cdr.detectChanges();

    try {
      const recommendationText = this.recommendations
        .map((r: any) => r.title ? `- ${r.title}` : '')
        .filter((s: string) => s.trim().length > 0)
        .join('\n');

      console.log('[FatigueResult] Saving result to backend...');
      await this.apiClient.post('/fatigue-results', {
        fatigueScore: Math.round(this.score),
        riskLabel: this.riskLabel,
        riskTitle: this.riskTitle,
        message: this.message,
        confidence: this.confidence,
        recommendations: this.recommendations,
        recommendationText: recommendationText
      });
      console.log('[FatigueResult] Result saved successfully');
    } catch (error: any) {
      console.error('[FatigueResult] Error saving result:', error);
      this.saveError = error.message || "Erreur lors de l'enregistrement du résultat fatigue.";
    } finally {
      this.saving = false;
      this.cdr.detectChanges();
    }
  }

  retakePhoto() {
    this.router.navigate(['/fatigue-camera']);
  }

  goHome() {
    this.router.navigate(['/user-home']);
  }
}

