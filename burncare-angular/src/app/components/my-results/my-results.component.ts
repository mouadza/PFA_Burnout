import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiClientService } from '../../services/api-client.service';

interface BurnoutResult {
  id: number;
  burnoutScore: number;
  riskLabel: string;
  riskTitle: string;
  createdAt: string | Date;
  message?: string;
  recommendation?: string;
}

interface FatigueResult {
  id: number;
  fatigueScore: number;
  riskLabel: string;
  riskTitle: string;
  createdAt: string | Date | number; // Can be Instant (number) or string
  message?: string;
  recommendationText?: string;
  recommendationsJson?: string;
  confidence?: number;
}

@Component({
  selector: 'app-my-results',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-results.component.html',
  styleUrl: './my-results.component.scss'
})
export class MyResultsComponent implements OnInit {
  activeTab: 'burnout' | 'fatigue' = 'burnout';
  
  loadingBurnout = true;
  loadingFatigue = true;
  errorBurnout: string | null = null;
  errorFatigue: string | null = null;
  
  burnoutResults: BurnoutResult[] = [];
  fatigueResults: FatigueResult[] = [];

  constructor(
    private router: Router,
    private apiClient: ApiClientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchBurnoutResults();
    this.fetchFatigueResults();
  }

  async fetchBurnoutResults() {
    this.loadingBurnout = true;
    this.errorBurnout = null;
    this.burnoutResults = [];
    this.cdr.detectChanges();

    try {
      const token = this.apiClient.getToken();
      if (!token) {
        this.errorBurnout = 'Utilisateur non authentifié. Veuillez vous reconnecter.';
        this.loadingBurnout = false;
        this.cdr.detectChanges();
        return;
      }

      console.log('[MyResults] Fetching burnout results...');
      const results = await this.apiClient.get<BurnoutResult[]>('/burnout-results/me');
      console.log('[MyResults] Burnout results received:', results);
      this.burnoutResults = Array.isArray(results) ? results : [];
    } catch (error: any) {
      console.error('[MyResults] Error fetching burnout results:', error);
      this.errorBurnout = error.message || 'Erreur lors du chargement des résultats burnout.';
      this.burnoutResults = [];
    } finally {
      this.loadingBurnout = false;
      this.cdr.detectChanges();
      console.log('[MyResults] Burnout loading set to false, results.length:', this.burnoutResults.length);
    }
  }

  async fetchFatigueResults() {
    this.loadingFatigue = true;
    this.errorFatigue = null;
    this.fatigueResults = [];
    this.cdr.detectChanges();

    try {
      const token = this.apiClient.getToken();
      if (!token) {
        this.errorFatigue = 'Utilisateur non authentifié. Veuillez vous reconnecter.';
        this.loadingFatigue = false;
        this.cdr.detectChanges();
        return;
      }

      console.log('[MyResults] Fetching fatigue results...');
      const results = await this.apiClient.get<FatigueResult[]>('/fatigue-results/me');
      console.log('[MyResults] Fatigue results received:', results);
      this.fatigueResults = Array.isArray(results) ? results : [];
    } catch (error: any) {
      console.error('[MyResults] Error fetching fatigue results:', error);
      this.errorFatigue = error.message || 'Erreur lors du chargement des résultats fatigue.';
      this.fatigueResults = [];
    } finally {
      this.loadingFatigue = false;
      this.cdr.detectChanges();
      console.log('[MyResults] Fatigue loading set to false, results.length:', this.fatigueResults.length);
    }
  }

  setTab(tab: 'burnout' | 'fatigue') {
    this.activeTab = tab;
  }

  formatDate(dateString: string | Date | number): string {
    try {
      let date: Date;
      
      if (typeof dateString === 'number') {
        // Unix timestamp in seconds or milliseconds
        date = dateString > 1000000000000 
          ? new Date(dateString) // milliseconds
          : new Date(dateString * 1000); // seconds
      } else if (typeof dateString === 'string') {
        date = new Date(dateString);
      } else {
        date = dateString;
      }
      
      if (isNaN(date.getTime())) {
        return 'Date invalide';
      }
      
      const dd = String(date.getDate()).padStart(2, '0');
      const mm = String(date.getMonth() + 1).padStart(2, '0');
      const yyyy = date.getFullYear();
      const hh = String(date.getHours()).padStart(2, '0');
      const min = String(date.getMinutes()).padStart(2, '0');
      return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
    } catch (e) {
      return 'Date invalide';
    }
  }

  getColorForRisk(riskLabel: string): string {
    switch (riskLabel) {
      case 'Faible':
        return '#27ae60';
      case 'Moyen':
        return '#f39c12';
      case 'Élevé':
      default:
        return '#e74c3c';
    }
  }

  openDetails(result: BurnoutResult | FatigueResult, type: 'burnout' | 'fatigue') {
    // Show details in a modal or navigate to detail page
    // For now, we'll just show an alert with details
    const details = type === 'burnout' 
      ? `Score: ${(result as BurnoutResult).burnoutScore}\nRisque: ${result.riskLabel}\nDate: ${this.formatDate(result.createdAt)}\n${result.message || ''}`
      : `Score: ${(result as FatigueResult).fatigueScore}\nRisque: ${result.riskLabel}\nConfiance: ${((result as FatigueResult).confidence || 0) * 100}%\nDate: ${this.formatDate(result.createdAt)}\n${result.message || ''}`;
    
    alert(details);
  }

  refresh() {
    this.fetchBurnoutResults();
    this.fetchFatigueResults();
  }

}
