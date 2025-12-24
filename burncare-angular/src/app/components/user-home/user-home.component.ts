import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ApiClientService } from '../../services/api-client.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-home.component.html',
  styleUrl: './user-home.component.scss'
})
export class UserHomeComponent implements OnInit, OnDestroy {
  userName = '';
  lastTestDate: string = '';
  averageScore: number = 0;
  testsThisMonth: number = 0;
  isLoading = true;
  private userUpdateListener?: (event: any) => void;

  constructor(
    private authService: AuthService, 
    private router: Router,
    private apiClient: ApiClientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadUserName();
    this.loadUserStats();
    
    // Listen for user updates
    this.userUpdateListener = (event: any) => {
      if (event.detail) {
        this.userName = event.detail.firstName || 'Utilisateur';
        this.cdr.detectChanges();
      }
    };
    window.addEventListener('userUpdated', this.userUpdateListener);
  }

  ngOnDestroy() {
    if (this.userUpdateListener) {
      window.removeEventListener('userUpdated', this.userUpdateListener);
    }
  }

  loadUserName() {
    // Get user name from localStorage
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      this.userName = user.firstName || 'Utilisateur';
    }
  }

  async loadUserStats() {
    this.isLoading = true;
    this.cdr.detectChanges();
    
    try {
      const token = this.apiClient.getToken();
      if (!token) {
        this.isLoading = false;
        this.cdr.detectChanges();
        return;
      }

      // Fetch burnout and fatigue results
      const [burnoutResults, fatigueResults] = await Promise.all([
        this.apiClient.get<any[]>('/burnout-results/me').catch(() => []),
        this.apiClient.get<any[]>('/fatigue-results/me').catch(() => [])
      ]);

      // Combine all results
      const allResults = [
        ...(Array.isArray(burnoutResults) ? burnoutResults : []),
        ...(Array.isArray(fatigueResults) ? fatigueResults : [])
      ];

      // Sort by date (most recent first)
      allResults.sort((a, b) => {
        const dateA = new Date(a.createdAt || 0).getTime();
        const dateB = new Date(b.createdAt || 0).getTime();
        return dateB - dateA;
      });

      // Last test date
      if (allResults.length > 0) {
        const lastTest = allResults[0];
        const lastTestDate = new Date(lastTest.createdAt);
        const now = new Date();
        const diffTime = Math.abs(now.getTime() - lastTestDate.getTime());
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffDays === 0) {
          this.lastTestDate = "Aujourd'hui";
        } else if (diffDays === 1) {
          this.lastTestDate = "Hier";
        } else {
          this.lastTestDate = `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
        }
      } else {
        this.lastTestDate = "Aucun test";
      }

      // Average score
      if (allResults.length > 0) {
        const scores = allResults
          .map(r => r.burnoutScore || r.fatigueScore || 0)
          .filter(s => s > 0);
        
        if (scores.length > 0) {
          this.averageScore = Math.round(
            scores.reduce((sum, s) => sum + s, 0) / scores.length
          );
        }
      }

      // Tests this month
      const now = new Date();
      const thisMonth = now.getMonth();
      const thisYear = now.getFullYear();
      
      this.testsThisMonth = allResults.filter(r => {
        const testDate = new Date(r.createdAt);
        return testDate.getMonth() === thisMonth && 
               testDate.getFullYear() === thisYear;
      }).length;

      console.log('[UserHome] Stats loaded:', {
        lastTestDate: this.lastTestDate,
        averageScore: this.averageScore,
        testsThisMonth: this.testsThisMonth
      });
    } catch (error: any) {
      console.error('[UserHome] Error loading stats:', error);
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }


  navigateToFatigueTest() {
    this.router.navigate(['/fatigue-camera']);
  }

  navigateToResults() {
    this.router.navigate(['/my-results']);
  }

  navigateToQuestionnaire() {
    this.router.navigate(['/questionnaire']);
  }

  navigateToSettings() {
    this.router.navigate(['/settings']);
  }
}
