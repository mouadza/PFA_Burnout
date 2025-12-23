import { Component, ChangeDetectorRef, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth-response.model';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  

  onSubmit() {
    // Clear previous error first
    this.errorMessage = '';
    
    // Mark all fields as touched to show validation errors if form is invalid
    if (!this.loginForm.valid) {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
      this.errorMessage = 'Veuillez remplir tous les champs correctement.';
      this.cdr.detectChanges(); // Force UI update
      return;
    }
    
    // Form is valid, proceed with login
    this.loading = true;
    const email = this.loginForm.value.email?.trim();
    const password = this.loginForm.value.password;

    this.authService.login(email, password)
      .then((response: AuthResponse) => {
        // Check role from response
        const role = (response?.role || '').toString().trim().toUpperCase();
        const profession = (response?.profession || '').toString().trim().toUpperCase();
        
        if (role === 'ADMIN' || profession === 'ADMIN') {
          this.router.navigate(['/admin-home']);
        } else {
          this.router.navigate(['/user-home']);
        }
      })
      .catch((error: any) => {
        console.error('[LoginComponent] Login error caught:', error);
        console.error('[LoginComponent] Error status:', error?.status);
        console.error('[LoginComponent] Error message:', error?.message);
        
        // Extract error message - Check message content FIRST (like Flutter does)
        let message = 'Email ou mot de passe incorrect';
        
        // Get the raw error message
        const rawError = error?.message || '';
        const lowerError = rawError.toLowerCase();
        
        // Priority 1: Check message content for disabled/activated account keywords (like Flutter)
        // This works regardless of HTTP status code
        if (lowerError.includes('disabl') ||
            lowerError.includes('lock') ||
            lowerError.includes('bloqu') ||
            lowerError.includes('desactiv') ||
            lowerError.includes('désactivé') ||
            lowerError.includes('activé') ||
            lowerError.includes('active') ||
            lowerError.includes('administrateur') ||
            lowerError.includes('banni') ||
            lowerError.includes('suspend') ||
            lowerError.includes('doit être')) {
          message = 'Votre compte doit être activé par un administrateur';
          console.log('[LoginComponent] Disabled account detected in message');
        }
        // Priority 2: Check status code
        else if (error && error.status === 403) {
          message = 'Votre compte doit être activé par un administrateur';
          console.log('[LoginComponent] 403 error detected - setting activation message');
        }
        // Priority 3: Check for 401 or incorrect credentials
        else if (lowerError.includes('401') ||
                 lowerError.includes('bad credentials') ||
                 lowerError.includes('session') ||
                 lowerError.includes('unauthorized') ||
                 lowerError.includes('incorrect')) {
          message = 'Email ou mot de passe incorrect';
          console.log('[LoginComponent] Incorrect credentials detected');
        }
        // Priority 4: Use error message if available
        else if (rawError) {
          message = rawError;
          console.log('[LoginComponent] Using raw error message:', message);
        } else if (typeof error === 'string') {
          message = error;
          console.log('[LoginComponent] Using error as string:', message);
        }
        
        // Always set and display the error message - MUST run in Angular zone
        this.loading = false;
        
        // Use requestAnimationFrame and NgZone to ensure Angular detects the change
        requestAnimationFrame(() => {
          this.ngZone.run(() => {
            this.errorMessage = message; // Set the message inside NgZone
            
            console.log('[LoginComponent] Final errorMessage set to:', this.errorMessage);
            console.log('[LoginComponent] errorMessage length:', this.errorMessage.length);
            console.log('[LoginComponent] errorMessage truthy?', !!this.errorMessage);
            console.log('[LoginComponent] errorMessage trim length:', this.errorMessage.trim().length);
            
            // Force UI update immediately
            this.cdr.detectChanges();
            
            // Force UI update again after a very short delay
            setTimeout(() => {
              this.ngZone.run(() => {
                this.cdr.markForCheck();
                this.cdr.detectChanges();
                
                console.log('[LoginComponent] UI update forced after timeout');
                console.log('[LoginComponent] errorMessage after timeout:', this.errorMessage);
                
                // Double check the DOM element exists
                setTimeout(() => {
                  const errorEl = document.querySelector('.error-message');
                  console.log('[LoginComponent] Error element in DOM?', !!errorEl);
                  if (errorEl) {
                    console.log('[LoginComponent] Error element innerHTML:', errorEl.innerHTML);
                    console.log('[LoginComponent] Error element textContent:', errorEl.textContent);
                    console.log('[LoginComponent] Error element computed style display:', window.getComputedStyle(errorEl).display);
                  } else {
                    console.warn('[LoginComponent] ERROR: Error element NOT found in DOM!');
                  }
                }, 50);
              });
            }, 50);
          });
        });
      });
  }

  hasErrorMessage(): boolean {
    return !!(this.errorMessage && this.errorMessage.trim().length > 0);
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}
