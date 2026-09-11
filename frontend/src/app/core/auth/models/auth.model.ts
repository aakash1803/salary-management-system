export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  username: string;
  expiresInSeconds: number;
}

export interface UserProfile {
  username: string;
}
