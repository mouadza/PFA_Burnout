import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-admin-user-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-user-details.component.html',
  styleUrl: './admin-user-details.component.scss'
})
export class AdminUserDetailsComponent implements OnInit {
  user: any = null;
  isLoading = true;
  isSaving = false;
  error: string | null = null;
  isApproved: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private adminService: AdminService,
    private cdr: ChangeDetectorRef
  ) {}

  async ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      await this.loadUser(id);
    }
  }

  async loadUser(id: string) {
    this.isLoading = true;
    this.error = null;
    this.user = null;
    this.cdr.detectChanges();
    
    try {
      console.log('[AdminUserDetails] Loading user:', id);
      this.user = await this.adminService.getUserById(id);
      this.isApproved = this.user?.enabled ?? false;
      console.log('[AdminUserDetails] User loaded:', this.user);
    } catch (error: any) {
      console.error('[AdminUserDetails] Error:', error);
      this.error = error.message || 'Erreur lors du chargement';
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
      console.log('[AdminUserDetails] Loading set to false, user:', this.user ? 'loaded' : 'null');
    }
  }

  async toggleApproval() {
    this.isSaving = true;
    this.cdr.detectChanges();
    
    try {
      await this.adminService.updateUser(this.user.id.toString(), {
        ...this.user,
        enabled: !this.isApproved
      });
      this.isApproved = !this.isApproved;
      this.user.enabled = this.isApproved;
      
      // If updating current user, update localStorage and notify layout
      const currentUserStr = localStorage.getItem('user');
      if (currentUserStr) {
        const currentUser = JSON.parse(currentUserStr);
        if (currentUser.email === this.user.email) {
          currentUser.enabled = this.user.enabled;
          currentUser.role = this.user.role;
          currentUser.profession = this.user.profession;
          localStorage.setItem('user', JSON.stringify(currentUser));
          window.dispatchEvent(new CustomEvent('userUpdated', { detail: currentUser }));
        }
      }
    } catch (error: any) {
      alert(`Erreur : ${error.message || error}`);
    } finally {
      this.isSaving = false;
      this.cdr.detectChanges();
    }
  }

}

