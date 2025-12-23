import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { LayoutComponent } from './components/layout/layout.component';
import { UserHomeComponent } from './components/user-home/user-home.component';
import { AdminHomeComponent } from './components/admin-home/admin-home.component';
import { FatigueCameraComponent } from './components/fatigue-camera/fatigue-camera.component';
import { MyResultsComponent } from './components/my-results/my-results.component';
import { QuestionnaireComponent } from './components/questionnaire/questionnaire.component';
import { QuestionnaireResultComponent } from './components/questionnaire-result/questionnaire-result.component';
import { FatigueResultComponent } from './components/fatigue-result/fatigue-result.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AdminStatsComponent } from './components/admin-stats/admin-stats.component';
import { AdminUserDetailsComponent } from './components/admin-user-details/admin-user-details.component';
import { SettingsComponent } from './components/settings/settings.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: 'user-home', component: UserHomeComponent },
      { path: 'admin-home', component: AdminHomeComponent },
      { path: 'fatigue-camera', component: FatigueCameraComponent },
      { path: 'fatigue-result', component: FatigueResultComponent },
      { path: 'my-results', component: MyResultsComponent },
      { path: 'questionnaire', component: QuestionnaireComponent },
      { path: 'questionnaire-result', component: QuestionnaireResultComponent },
      { path: 'admin-users', component: AdminUsersComponent },
      { path: 'admin-stats', component: AdminStatsComponent },
      { path: 'admin-user-details/:id', component: AdminUserDetailsComponent },
      { path: 'settings', component: SettingsComponent },
    ]
  },
  { path: '**', redirectTo: 'login' }
];

