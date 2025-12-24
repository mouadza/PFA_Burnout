import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
  console.log('✅ HomeComponent loaded');

  const token = this.authService.getToken();
  if (token) {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const profession = (payload.profession || '').trim().toUpperCase();

    console.log('Token payload:', payload);
    console.log('Profession:', profession);

    if (profession === 'ADMIN') {
      this.router.navigate(['/admin-home']);
    } else {
      this.router.navigate(['/user-home']);
    }
  } else {
    console.log('❌ No token -> redirect to login');
    this.router.navigate(['/login']);
  }
}


  navigateToLogin() {
    this.router.navigate(['/login']);
  }

  navigateToRegister() {
    this.router.navigate(['/register']);
  }

  scrollToFeatures() {
    const element = document.getElementById('features');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
