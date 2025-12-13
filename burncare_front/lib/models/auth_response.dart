class AuthResponse {
  final String token;
  final String firstName;
  final String lastName;
  final String email;
  final String role;
  final String profession;

  AuthResponse({
    required this.token,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.role,
    required this.profession,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'] ?? '',
      firstName: json['firstName'] ?? '',
      lastName: json['lastName'] ?? '',
      email: json['email'] ?? '',
      role: json['role'] ?? 'USER',
      profession: json['profession'] ?? '',
    );
  }
}