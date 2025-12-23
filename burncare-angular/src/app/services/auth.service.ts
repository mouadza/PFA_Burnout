import { Injectable } from '@angular/core';
import { ApiClientService } from './api-client.service';
import { AuthResponse } from '../models/auth-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private apiClient: ApiClientService) {}

  async login(email: string, password: string): Promise<AuthResponse> {
    try {
      const response = await this.apiClient.post<AuthResponse>('/auth/login', { email, password });
      
      // Validate response
      if (!response) {
        throw new Error('Réponse invalide du serveur');
      }
      
      if (response && response.token) {
        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify({
          firstName: response.firstName || '',
          lastName: response.lastName || '',
          email: response.email || email,
          role: response.role || 'USER',
          profession: response.profession || ''
        }));
      } else {
        throw new Error('Token manquant dans la réponse');
      }
      
      return response;
    } catch (error: any) {
      console.error('[AuthService] Login error:', error);
      console.error('[AuthService] Error status:', error?.status);
      console.error('[AuthService] Error message:', error?.message);
      
      // Extract message from error
      let errorMessage = 'Email ou mot de passe incorrect';
      
      // Get error message first
      const errorMsg = error?.message || '';
      const errorMsgLower = errorMsg.toLowerCase();
      
      // Priority 1: Check if message indicates disabled/activated account (regardless of status)
      // Backend might return 401 but message contains activation keywords
      if (errorMsgLower.includes('activé') || 
          errorMsgLower.includes('active') ||
          errorMsgLower.includes('administrateur') ||
          errorMsgLower.includes('désactivé') ||
          errorMsgLower.includes('disabled') ||
          errorMsgLower.includes('doit être')) {
        errorMessage = 'Votre compte doit être activé par un administrateur';
      }
      // Priority 2: Check status code
      else if (error && error.status === 403) {
        errorMessage = 'Votre compte doit être activé par un administrateur';
      } else if (error && error.status === 401) {
        // Even for 401, check if message contains activation keywords
        if (errorMsgLower.includes('activé') || errorMsgLower.includes('administrateur')) {
          errorMessage = 'Votre compte doit être activé par un administrateur';
        } else {
          errorMessage = errorMsg || 'Email ou mot de passe incorrect';
        }
      } else if (error && error.message) {
        // Priority 3: Use error message if available
        errorMessage = error.message;
      } else if (typeof error === 'string') {
        errorMessage = error;
      }
      
      console.log('[AuthService] Final error message:', errorMessage);
      
      // Create a new error with the message and status
      const loginError = new Error(errorMessage);
      (loginError as any).status = error?.status || 500;
      throw loginError;
    }
  }

  async register(data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    profession: string;
    role?: string;
  }): Promise<void> {
    try {
      const response = await this.apiClient.post<AuthResponse>('/auth/register', {
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
        profession: data.profession,
        role: data.role || 'USER',
        enabled: false
      });
      // Registration successful, user needs admin approval
      return;
    } catch (error: any) {
      console.error('Register error:', error);
      // Extract message from error
      let errorMessage = 'Erreur lors de l\'inscription. Cet email est peut-être déjà utilisé.';
      if (error.message) {
        errorMessage = error.message;
      } else if (typeof error === 'string') {
        errorMessage = error;
      }
      throw new Error(errorMessage);
    }
  }

  async testConnection(): Promise<{ status: string; message: string }> {
    try {
      // Test simple de connexion en essayant d'accéder à l'endpoint de login avec des données invalides
      // Cela nous permet de vérifier si le serveur répond sans créer de session
      const response = await fetch(`${this.apiClient.getSpringApiUrl()}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email: 'test@test.com', password: 'test' })
      });
      
      // Si on reçoit une réponse (même erreur), le serveur est accessible
      if (response.status === 401 || response.status === 400) {
        return {
          status: 'success',
          message: 'Connexion au serveur réussie'
        };
      } else if (response.ok) {
        return {
          status: 'success',
          message: 'Connexion au serveur réussie'
        };
      } else {
        return {
          status: 'error',
          message: 'Impossible de se connecter au serveur'
        };
      }
    } catch (error: any) {
      return {
        status: 'error',
        message: 'Erreur de connexion: Serveur inaccessible. Vérifiez que le serveur est démarré.'
      };
    }
  }

  async updateProfile(email: string, firstName: string, lastName: string): Promise<AuthResponse> {
    const response = await this.apiClient.put<AuthResponse>('/user/profile', { email, firstName, lastName });
    
    // Update localStorage with new user data
    if (response) {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        user.firstName = response.firstName || firstName;
        user.lastName = response.lastName || lastName;
        user.email = response.email || email;
        localStorage.setItem('user', JSON.stringify(user));
        
        // Dispatch custom event to notify other components
        window.dispatchEvent(new CustomEvent('userUpdated', { detail: user }));
      }
    }
    
    return response;
  }

  async changePassword(email: string, newPassword: string): Promise<void> {
    await this.apiClient.put('/user/password', { email, newPassword });
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    // Dispatch event to notify components
    window.dispatchEvent(new CustomEvent('userLoggedOut'));
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
