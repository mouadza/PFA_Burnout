import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';
import { ApiClientService } from '../../services/api-client.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-home.component.html',
  styleUrl: './admin-home.component.scss'
})
export class AdminHomeComponent implements OnInit {
  users: any[] = [];
  totalUsers = 0;
  activeUsers = 0;
  testsToday = 0;
  isLoading = true;

  constructor(
    private authService: AuthService,
    private adminService: AdminService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private apiClient: ApiClientService
  ) {}

  ngOnInit() {
    this.loadUsers();
    this.loadStats();
  }

  async loadUsers() {
    this.users = [];
    this.totalUsers = 0;
    this.activeUsers = 0;
    this.cdr.detectChanges();
    
    try {
      console.log('[AdminHome] Loading users...');
      this.users = await this.adminService.getAllUsers();
      this.totalUsers = this.users.length;
      this.activeUsers = this.users.filter(u => u.enabled).length;
      console.log('[AdminHome] Users loaded:', this.users.length);
    } catch (error: any) {
      console.error('[AdminHome] Error loading users:', error);
    } finally {
      this.cdr.detectChanges();
    }
  }

  async loadStats() {
    this.isLoading = true;
    this.cdr.detectChanges();
    
    try {
      // Get statistics from admin stats endpoint
      const stats = await this.adminService.getStatistics();
      if (stats) {
        // Calculate tests today from burnout and fatigue results
        // We'll need to fetch all results and filter by today
        this.testsToday = (stats.burnoutTotal || 0) + (stats.fatigueTotal || 0);
        // For now, we'll use a simple calculation, but ideally we'd filter by date
      }
    } catch (error: any) {
      console.error('[AdminHome] Error loading stats:', error);
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  viewUserDetails(user: any) {
    this.router.navigate(['/admin-user-details', user.id]);
  }

  navigateToUsers() {
    this.router.navigate(['/admin-users']);
  }

  navigateToStats() {
    this.router.navigate(['/admin-stats']);
  }

  navigateToSettings() {
    this.router.navigate(['/settings']);
  }
}
