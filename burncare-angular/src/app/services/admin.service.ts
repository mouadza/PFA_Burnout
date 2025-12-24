import { Injectable } from '@angular/core';
import { ApiClientService } from './api-client.service';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(private apiClient: ApiClientService) { }

  async getAllUsers(): Promise<any[]> {
    try {
      console.log('[AdminService] Fetching all users...');
      const response = await this.apiClient.get('/admin/users');
      console.log('[AdminService] Users received:', response);
      return Array.isArray(response) ? response : [];
    } catch (error: any) {
      console.error('[AdminService] Error fetching users:', error);
      throw error;
    }
  }

  async getUserById(id: string): Promise<any> {
    try {
      // Backend doesn't have GET /admin/users/{id}, so we fetch all and filter
      console.log('[AdminService] Fetching user by ID:', id);
      const allUsers = await this.getAllUsers();
      const user = allUsers.find((u: any) => u.id?.toString() === id.toString());
      if (!user) {
        throw new Error(`Utilisateur avec l'ID ${id} non trouvé`);
      }
      return user;
    } catch (error: any) {
      console.error('[AdminService] Error fetching user:', error);
      throw error;
    }
  }

  async updateUser(id: string, userData: any): Promise<any> {
    try {
      const response = await this.apiClient.put(`/admin/users/${id}`, userData);
      return response;
    } catch (error) {
      console.error('Error updating user:', error);
      throw error;
    }
  }

  async deleteUser(id: string): Promise<any> {
    try {
      const response = await this.apiClient.delete(`/admin/users/${id}`);
      return response;
    } catch (error) {
      console.error('Error deleting user:', error);
      throw error;
    }
  }

  async getStatistics(): Promise<any> {
    try {
      console.log('[AdminService] Fetching statistics...');
      const response = await this.apiClient.get('/admin/stats');
      console.log('[AdminService] Statistics received:', response);
      return response;
    } catch (error: any) {
      console.error('[AdminService] Error fetching statistics:', error);
      throw error;
    }
  }

  async addUser(userData: any): Promise<any> {
    try {
      const response = await this.apiClient.post('/auth/register', userData);
      return response;
    } catch (error) {
      console.error('Error adding user:', error);
      throw error;
    }
  }
}
