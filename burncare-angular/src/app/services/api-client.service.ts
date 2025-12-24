import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ApiClientService {
  private baseUrl = 'http://localhost:8080/api'; // Spring Boot API
  private fastApiUrl = 'http://localhost:8000'; // FastAPI

  constructor() {}

  getFastApiUrl(): string {
    return this.fastApiUrl;
  }

  getSpringApiUrl(): string {
    return this.baseUrl;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  async post<T>(path: string, body: any): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const token = localStorage.getItem('token');
    
    // Don't send token for auth endpoints
    const isAuthEndpoint = path.includes('/auth/login') || path.includes('/auth/register');

    console.log(`[API] POST ${url}`, token && !isAuthEndpoint ? 'with token' : 'no token');

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && !isAuthEndpoint && { 'Authorization': `Bearer ${token}` })
      },
      body: JSON.stringify(body)
    });

    console.log(`[API] POST ${url} - Status: ${response.status}`);

    return this.handleResponse<T>(response);
  }

  async put<T>(path: string, body: any): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const token = localStorage.getItem('token');

    const response = await fetch(url, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
      },
      body: JSON.stringify(body)
    });

    return this.handleResponse<T>(response);
  }

  async get<T>(path: string): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const token = localStorage.getItem('token');

    console.log(`[API] GET ${url}`, token ? 'with token' : 'no token');

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
      }
    });

    console.log(`[API] GET ${url} - Status: ${response.status}`);

    return this.handleResponse<T>(response);
  }

  async delete<T>(path: string): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const token = localStorage.getItem('token');

    const response = await fetch(url, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` })
      }
    });

    return this.handleResponse<T>(response);
  }

  async postFile<T>(url: string, file: File, additionalFields?: Record<string, string>): Promise<T> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (additionalFields) {
      Object.keys(additionalFields).forEach(key => {
        formData.append(key, additionalFields[key]);
      });
    }

    const token = localStorage.getItem('token');
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        ...(token && { 'Authorization': `Bearer ${token}` })
      },
      body: formData
    });

    return this.handleResponse<T>(response);
  }

  async postToFastApi<T>(path: string, body: any): Promise<T> {
    const url = `${this.fastApiUrl}${path}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(body)
    });

    return this.handleResponse<T>(response);
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    
    if (response.ok) {
      if (isJson) {
        const data = await response.json();
        return data as T;
      }
      const text = await response.text();
      return text as any;
    } else {
      // Try to parse error as JSON first (like Flutter does)
      let errorMessage = '';
      
      try {
        // Read the response body as text first
        const errorText = await response.text();
        console.log(`[API] Error response body (${response.status}):`, errorText);
        
        if (errorText && errorText.trim()) {
          // Try to parse as JSON
          try {
            const errorJson = JSON.parse(errorText);
            console.log('[API] Parsed error JSON:', errorJson);
            
            // Backend might return { message: "..." } or { error: "..." } format (like Flutter checks)
            if (errorJson.message) {
              errorMessage = errorJson.message;
              console.log('[API] Extracted message from errorJson.message:', errorMessage);
            } else if (errorJson.error) {
              errorMessage = errorJson.error;
              console.log('[API] Extracted message from errorJson.error:', errorMessage);
            } else if (typeof errorJson === 'string') {
              errorMessage = errorJson;
              console.log('[API] ErrorJson is a string:', errorMessage);
            } else {
              // Try to find any string value in the object
              const messageValue = Object.values(errorJson).find(v => typeof v === 'string');
              errorMessage = (messageValue as string) || errorText;
              console.log('[API] Extracted message from object values:', errorMessage);
            }
          } catch (parseError) {
            // Not JSON, use the text as is
            errorMessage = errorText;
            console.log('[API] Error response is not JSON, using text as is:', errorMessage);
          }
        }
      } catch (e) {
        console.error('[API] Error reading response body:', e);
      }
      
      console.log('[API] Final extracted errorMessage before keyword check:', errorMessage);
      
      // Provide specific messages for common status codes if no message was extracted
      if (!errorMessage || errorMessage.includes('HTTP')) {
        if (response.status === 403) {
          errorMessage = 'Votre compte doit être activé par un administrateur';
        } else if (response.status === 401) {
          // For 401, check if it's a disabled account by looking at the message
          // Backend might return 401 with a message about account activation
          errorMessage = 'Email ou mot de passe incorrect';
        } else if (response.status === 404) {
          errorMessage = 'Ressource non trouvée.';
        } else if (response.status === 409) {
          errorMessage = 'Cet email est déjà utilisé';
        } else if (response.status === 500) {
          errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
        } else {
          errorMessage = errorMessage || `Erreur HTTP ${response.status}`;
        }
      }
      
      // Check if the error message indicates a disabled account (even if status is 401)
      // Backend may return different status codes but the message should contain keywords
      // Use same keywords as Flutter: disabl, lock, bloqu, desactiv, activé, administrateur, etc.
      const errorMsgLower = errorMessage.toLowerCase();
      if (errorMessage && (
        errorMsgLower.includes('disabl') ||
        errorMsgLower.includes('lock') ||
        errorMsgLower.includes('bloqu') ||
        errorMsgLower.includes('desactiv') ||
        errorMsgLower.includes('désactivé') ||
        errorMsgLower.includes('activé') ||
        errorMsgLower.includes('active') ||
        errorMsgLower.includes('administrateur') ||
        errorMsgLower.includes('banni') ||
        errorMsgLower.includes('suspend') ||
        errorMsgLower.includes('doit être')
      )) {
        // Ensure we use the correct message for disabled account
        errorMessage = 'Votre compte doit être activé par un administrateur';
        console.log('[API] Disabled account detected in message, setting activation message');
      }
      
      console.error(`[API] Error ${response.status}: ${errorMessage}`);
      
      const error = new Error(errorMessage);
      (error as any).status = response.status;
      throw error;
    }
  }
}
