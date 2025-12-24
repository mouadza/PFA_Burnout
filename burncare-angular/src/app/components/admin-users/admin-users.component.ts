import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent implements OnInit {
  users: any[] = [];
  isLoading = true;
  error: string | null = null;
  showAddDialog = false;
  showDeleteDialog = false;
  userToDelete: any = null;
  deleteInProgress = false;

  newUser = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'USER',
    profession: 'MEDECIN',
    enabled: true
  };

  roles = ['USER', 'ADMIN'];
  professions = ['MEDECIN', 'INFIRMIER', 'ADMIN'];

  constructor(
    private adminService: AdminService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.fetchUsers();
  }

  async fetchUsers() {
    this.isLoading = true;
    this.error = null;
    this.users = [];
    this.cdr.detectChanges();
    
    try {
      console.log('[AdminUsers] Fetching users...');
      const users = await this.adminService.getAllUsers();
      console.log('[AdminUsers] Users received:', users);
      
      // Trier les utilisateurs par ID décroissant (les plus récents en premier)
      // Comme l'entité User n'a pas de createdAt, on utilise l'ID comme proxy
      // Les IDs plus grands sont généralement créés plus récemment (auto-increment)
      this.users = Array.isArray(users) ? users.sort((a, b) => {
        const idA = a.id || 0;
        const idB = b.id || 0;
        return idB - idA; // Décroissant (plus récent en premier)
      }) : [];
      console.log('[AdminUsers] Users array set, length:', this.users.length);
    } catch (error: any) {
      console.error('[AdminUsers] Error:', error);
      this.error = error.message || 'Erreur lors du chargement des utilisateurs';
      this.users = [];
    } finally {
      this.isLoading = false;
      this.cdr.detectChanges();
      console.log('[AdminUsers] isLoading set to false, users.length:', this.users.length);
    }
  }

  openDeleteDialog(user: any) {
    this.userToDelete = user;
    this.showDeleteDialog = true;
  }

  closeDeleteDialog() {
    this.showDeleteDialog = false;
    this.userToDelete = null;
  }

  async deleteUser() {
    if (!this.userToDelete) {
      return;
    }

    this.deleteInProgress = true;
    this.cdr.detectChanges();

    try {
      await this.adminService.deleteUser(this.userToDelete.id.toString());
      this.closeDeleteDialog();
      this.fetchUsers();
    } catch (error: any) {
      console.error('[AdminUsers] Error deleting user:', error);
      alert(`Erreur lors de la suppression: ${error.message || error}`);
    } finally {
      this.deleteInProgress = false;
      this.cdr.detectChanges();
    }
  }

  async addUser() {
    try {
      await this.adminService.addUser(this.newUser);
      this.showAddDialog = false;
      this.resetNewUser();
      this.fetchUsers();
    } catch (error: any) {
      alert(`Erreur lors de l'ajout: ${error.message || error}`);
    }
  }

  resetNewUser() {
    this.newUser = {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      role: 'USER',
      profession: 'MEDECIN',
      enabled: true
    };
  }

  viewUserDetails(user: any) {
    this.router.navigate(['/admin-user-details', user.id]);
  }

}


