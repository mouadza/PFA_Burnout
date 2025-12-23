import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})
export class SettingsComponent implements OnInit {
  isEditing = false;
  isLoading = false;
  showPasswordDialog = false;

  profile = {
    firstName: '',
    lastName: '',
    email: '',
    profession: ''
  };

  passwordData = {
    newPassword: '',
    confirmPassword: ''
  };

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // Load user data from localStorage or service
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      this.profile = {
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        profession: user.profession || ''
      };
    }
  }

  async saveProfile() {
    this.isLoading = true;
    try {
      const response = await this.authService.updateProfile(
        this.profile.email,
        this.profile.firstName,
        this.profile.lastName
      );
      
      // Update local profile data
      if (response) {
        this.profile.firstName = response.firstName || this.profile.firstName;
        this.profile.lastName = response.lastName || this.profile.lastName;
        this.profile.email = response.email || this.profile.email;
      }
      
      this.isEditing = false;
      alert('Profil mis à jour avec succès !');
    } catch (error: any) {
      alert(`Erreur lors de la mise à jour: ${error.message || error}`);
    } finally {
      this.isLoading = false;
    }
  }

  async changePassword() {
    if (this.passwordData.newPassword !== this.passwordData.confirmPassword) {
      alert('Les mots de passe ne correspondent pas');
      return;
    }

    if (this.passwordData.newPassword.length < 4) {
      alert('Le mot de passe est trop court');
      return;
    }

    this.isLoading = true;
    try {
      await this.authService.changePassword(
        this.profile.email,
        this.passwordData.newPassword
      );
      this.showPasswordDialog = false;
      this.passwordData = { newPassword: '', confirmPassword: '' };
      alert('Mot de passe modifié !');
    } catch (error: any) {
      alert(`Erreur technique: ${error.message || error}`);
    } finally {
      this.isLoading = false;
    }
  }

}

