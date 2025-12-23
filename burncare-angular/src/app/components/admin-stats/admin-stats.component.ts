import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-admin-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-stats.component.html',
  styleUrl: './admin-stats.component.scss'
})
export class AdminStatsComponent implements OnInit {
  loading = true;
  stats: any = null;
  error: string | null = null;

  constructor(
    private adminService: AdminService,
    private cdr: ChangeDetectorRef
  ) {}

  async ngOnInit() {
    await this.fetchStats();
  }

  async fetchStats() {
    this.loading = true;
    this.error = null;
    this.stats = null;
    this.cdr.detectChanges();
    
    try {
      console.log('[AdminStats] Fetching stats...');
      this.stats = await this.adminService.getStatistics();
      console.log('[AdminStats] Stats received:', this.stats);
    } catch (error: any) {
      console.error('[AdminStats] Error:', error);
      this.error = error.message || 'Erreur lors du chargement des statistiques';
      this.stats = null;
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
      console.log('[AdminStats] loading set to false, stats:', this.stats);
    }
  }
}

