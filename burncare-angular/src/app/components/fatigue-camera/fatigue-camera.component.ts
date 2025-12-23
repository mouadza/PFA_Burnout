import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiClientService } from '../../services/api-client.service';

@Component({
  selector: 'app-fatigue-camera',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fatigue-camera.component.html',
  styleUrl: './fatigue-camera.component.scss'
})
export class FatigueCameraComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('videoElement', { static: false }) videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvasElement', { static: false }) canvasElement!: ElementRef<HTMLCanvasElement>;

  isCameraActive = false;
  isAnalyzing = false;
  isLoading = true;
  error: string | null = null;
  stream: MediaStream | null = null;

  constructor(
    private router: Router,
    private apiClient: ApiClientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Don't start camera here, wait for view to be initialized
  }

  ngAfterViewInit() {
    // Start camera after view is initialized so ViewChild is available
    setTimeout(() => {
      this.startCamera();
    }, 100);
  }

  ngOnDestroy() {
    this.stopCamera();
  }

  async startCamera() {
    this.isLoading = true;
    this.error = null;
    this.isCameraActive = false;
    this.cdr.detectChanges();
    
    try {
      console.log('[FatigueCamera] Requesting camera access...');
      
      // Check if getUserMedia is available
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('getUserMedia n\'est pas supporté par ce navigateur');
      }
      
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { 
          facingMode: 'user', 
          width: { ideal: 640 }, 
          height: { ideal: 480 } 
        }
      });
      
      console.log('[FatigueCamera] Camera stream obtained');
      
      if (this.videoElement?.nativeElement) {
        const video = this.videoElement.nativeElement;
        video.srcObject = this.stream;
        
        // Wait for video to be ready
        video.onloadedmetadata = () => {
          console.log('[FatigueCamera] Video metadata loaded');
          video.play().then(() => {
            console.log('[FatigueCamera] Video playing');
            this.isCameraActive = true;
            this.isLoading = false;
            this.cdr.detectChanges();
          }).catch((err) => {
            console.error('[FatigueCamera] Error playing video:', err);
            this.error = `Erreur lors de la lecture de la vidéo: ${err.message}`;
            this.isLoading = false;
            this.cdr.detectChanges();
          });
        };
        
        video.onerror = (err) => {
          console.error('[FatigueCamera] Video error:', err);
          this.error = 'Erreur lors du chargement de la vidéo';
          this.isLoading = false;
          this.cdr.detectChanges();
        };
      } else {
        throw new Error('Élément vidéo non trouvé');
      }
    } catch (error: any) {
      console.error('[FatigueCamera] Error accessing camera:', error);
      this.error = `Impossible d'accéder à la caméra: ${error.message || error}`;
      this.isCameraActive = false;
      this.isLoading = false;
      this.cdr.detectChanges();
    }
  }

  stopCamera() {
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }
    if (this.videoElement?.nativeElement) {
      this.videoElement.nativeElement.srcObject = null;
    }
    this.isCameraActive = false;
  }

  async captureAndAnalyze() {
    if (!this.isCameraActive || !this.videoElement || !this.canvasElement) {
      return;
    }

    this.isAnalyzing = true;
    this.error = null;

    try {
      const video = this.videoElement.nativeElement;
      const canvas = this.canvasElement.nativeElement;
      
      // Set canvas dimensions to match video
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      
      // Draw video frame to canvas
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        throw new Error('Could not get canvas context');
      }
      
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      
      // Convert canvas to blob
      canvas.toBlob(async (blob) => {
        if (!blob) {
          this.isAnalyzing = false;
          this.error = 'Erreur lors de la capture de l\'image';
          return;
        }

        try {
          // Convert blob to File
          const file = new File([blob], 'capture.jpg', { type: 'image/jpeg' });
          
          // Send to FastAPI
          const fastApiUrl = this.apiClient.getFastApiUrl();
          const formData = new FormData();
          formData.append('file', file);
          formData.append('context', JSON.stringify({
            role: 'Infirmier',
            department: 'Urgences',
            shift: 'Nuit',
            hours_slept: 5.5,
            stress_level: 8,
            had_breaks: false,
            caffeine_cups: 3,
            consecutive_shifts: 4
          }));

          const response = await fetch(`${fastApiUrl}/fatigue/predict_personalized`, {
            method: 'POST',
            headers: {
              'Accept': 'application/json'
            },
            body: formData
          });

          if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Erreur API: ${response.status}\n${errorText}`);
          }

          const data = await response.json();
          
          console.log('[FatigueCamera] Response received:', data);
          
          // Prepare navigation data
          const navigationData = {
            score: data.fatigue_score || 0,
            confidence: data.confidence || 0,
            riskTitle: data.risk_title || 'Résultat',
            riskLabel: data.risk_label || 'Moyen',
            message: data.message || '',
            recommendations: Array.isArray(data.personalized_recommendations) 
              ? data.personalized_recommendations 
              : [],
            imageDataUrl: canvas.toDataURL('image/jpeg')
          };
          
          console.log('[FatigueCamera] Navigating with data:', navigationData);
          
          // Navigate to result page
          this.router.navigate(['/fatigue-result'], {
            state: navigationData
          });
        } catch (error: any) {
          console.error('Error analyzing fatigue:', error);
          this.error = `Erreur: ${error.message || error}`;
          this.isAnalyzing = false;
        }
      }, 'image/jpeg', 0.9);
    } catch (error: any) {
      console.error('Error capturing image:', error);
      this.error = `Erreur lors de la capture: ${error.message || error}`;
      this.isAnalyzing = false;
    }
  }

}
