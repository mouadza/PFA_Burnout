class AuthResponse {
  final String token;
  final String fullName;
  final String email;
  final String role;
  final String profession;

  AuthResponse({
    required this.token,
    required this.fullName,
    required this.email,
    required this.role,
    required this.profession,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      token: json['token'],
      fullName: json['fullName'],
      email: json['email'],
      role: json['role'],
      profession: json['profession'],
    );
  }
}
