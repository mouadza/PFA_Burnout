import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  registerForm: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      profession: ['', [Validators.required]]
    });
  }

  async onSubmit() {
    if (this.registerForm.valid) {
      this.loading = true;
      this.errorMessage = '';

      try {
        const formData = this.registerForm.value;
        await this.authService.register({
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          password: formData.password,
          profession: formData.profession,
          role: 'USER'
        });
        alert('Inscription réussie ! Votre compte doit être approuvé par un administrateur avant de pouvoir vous connecter.');
        this.router.navigate(['/login']);
      } catch (error: any) {
        console.error('Register error:', error);
        // Extract error message properly
        let message = 'Une erreur est survenue lors de l\'inscription';
        if (error && error.message) {
          message = error.message;
        } else if (typeof error === 'string') {
          message = error;
        }
        this.errorMessage = message;
      } finally {
        this.loading = false;
      }
    } else {
      this.errorMessage = 'Veuillez remplir tous les champs correctement.';
    }
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
