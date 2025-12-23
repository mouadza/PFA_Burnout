import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit, OnDestroy {
  sidebarOpen = true;
  mobileMenuOpen = false;
  currentRoute = '';
  user: any = null;
  isAdmin = false;

  menuItems: any[] = [];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.currentRoute = event.url;
    });
  }

  private userUpdateListener?: (event: any) => void;

  ngOnInit() {
    this.loadUser();
    this.setupMenu();
    
    // Listen for user updates
    this.userUpdateListener = (event: any) => {
      if (event.detail) {
        console.log('[Layout] User updated event received:', event.detail);
        this.user = event.detail;
        this.isAdmin = this.user.role === 'ADMIN' || this.user.profession === 'ADMIN';
        this.setupMenu();
      }
    };
    window.addEventListener('userUpdated', this.userUpdateListener);
  }

  ngOnDestroy() {
    if (this.userUpdateListener) {
      window.removeEventListener('userUpdated', this.userUpdateListener);
    }
  }

  loadUser() {
    // First try to load from localStorage (most up-to-date)
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        this.user = JSON.parse(userStr);
        this.isAdmin = this.user.role === 'ADMIN' || this.user.profession === 'ADMIN';
        return;
      } catch (e) {
        console.error('Error parsing user from localStorage:', e);
      }
    }
    
    // Fallback: parse from token
    const token = this.authService.getToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.user = {
          firstName: payload.firstName || payload.given_name || '',
          lastName: payload.lastName || payload.family_name || '',
          email: payload.email || payload.preferred_username || '',
          role: payload.role || payload.realm_access?.roles?.[0] || 'USER',
          profession: payload.profession || ''
        };
        this.isAdmin = this.user.role === 'ADMIN' || this.user.profession === 'ADMIN';
        localStorage.setItem('user', JSON.stringify(this.user));
      } catch (e) {
        console.error('Error parsing token:', e);
      }
    }
  }

  setupMenu() {
    if (this.isAdmin) {
      this.menuItems = [
        { path: '/admin-home', icon: 'fa-home', label: 'Tableau de bord', exact: true },
        { path: '/admin-users', icon: 'fa-users', label: 'Gestion Utilisateurs' },
        { path: '/admin-stats', icon: 'fa-chart-bar', label: 'Statistiques' },
        { path: '/settings', icon: 'fa-cog', label: 'Paramètres' }
      ];
    } else {
      this.menuItems = [
        { path: '/user-home', icon: 'fa-home', label: 'Tableau de bord', exact: true },
        { path: '/questionnaire', icon: 'fa-clipboard-list', label: 'Questionnaire' },
        { path: '/fatigue-camera', icon: 'fa-camera', label: 'Détection Fatigue' },
        { path: '/my-results', icon: 'fa-chart-line', label: 'Mes Résultats' },
        { path: '/settings', icon: 'fa-cog', label: 'Paramètres' }
      ];
    }
  }

  toggleSidebar() {
    if (window.innerWidth <= 768) {
      this.mobileMenuOpen = !this.mobileMenuOpen;
    } else {
      this.sidebarOpen = !this.sidebarOpen;
    }
  }

  navigate(path: string) {
    this.router.navigate([path]);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isActive(path: string, exact: boolean = false): boolean {
    if (exact) {
      return this.currentRoute === path;
    }
    return this.currentRoute.startsWith(path);
  }

  getPageTitle(): string {
    const route = this.currentRoute;
    if (route.includes('admin-home')) return 'Tableau de bord Admin';
    if (route.includes('user-home')) return 'Tableau de bord';
    if (route.includes('questionnaire')) return 'Questionnaire Burnout';
    if (route.includes('fatigue-camera')) return 'Détection Fatigue';
    if (route.includes('my-results')) return 'Mes Résultats';
    if (route.includes('admin-users')) return 'Gestion Utilisateurs';
    if (route.includes('admin-stats')) return 'Statistiques';
    if (route.includes('admin-user-details')) return 'Détails Utilisateur';
    if (route.includes('settings')) return 'Paramètres';
    if (route.includes('questionnaire-result')) return 'Résultats Questionnaire';
    if (route.includes('fatigue-result')) return 'Résultats Fatigue';
    return 'BurnCare';
  }
}

