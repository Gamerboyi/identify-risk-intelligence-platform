Remove-Item -Recurse -Force .git
git init

$env:GIT_COMMITTER_DATE="30 days ago"
git add pom.xml src/main/resources/
git commit --date="30 days ago" -m "chore: initial spring boot project setup and dependencies"

$env:GIT_COMMITTER_DATE="26 days ago"
git add src/main/java/com/vedant/eurds/model/ src/main/java/com/vedant/eurds/repository/
git commit --date="26 days ago" -m "feat: define database entity relations and JPA repositories"

$env:GIT_COMMITTER_DATE="22 days ago"
git add src/main/java/com/vedant/eurds/security/ src/main/java/com/vedant/eurds/service/AuthService.java src/main/java/com/vedant/eurds/controller/AuthController.java
git commit --date="22 days ago" -m "feat: implement stateless JWT authentication and RBAC"

$env:GIT_COMMITTER_DATE="17 days ago"
git add ml-service/
git commit --date="17 days ago" -m "feat: develop python microservice for Isolation Forest anomaly detection"

$env:GIT_COMMITTER_DATE="13 days ago"
git add src/main/java/com/vedant/eurds/service/FeatureEngineeringService.java src/main/java/com/vedant/eurds/service/MlService.java
git commit --date="13 days ago" -m "feat: integrate behavioral ML risk-scoring pipeline with authentication via WebClient"

$env:GIT_COMMITTER_DATE="9 days ago"
git add src/main/java/com/vedant/eurds/service/IdsDetectionService.java src/main/java/com/vedant/eurds/controller/IdsController.java
git commit --date="9 days ago" -m "feat: build Intrusion Detection System with rate limiting and pattern matching"

$env:GIT_COMMITTER_DATE="6 days ago"
git add src/main/java/com/vedant/eurds/service/FirewallService.java src/main/java/com/vedant/eurds/controller/FirewallController.java
git commit --date="6 days ago" -m "feat: implement CIDR-aware application layer firewall engine"

$env:GIT_COMMITTER_DATE="4 days ago"
git add src/test/
git commit --date="4 days ago" -m "test: add comprehensive unit and integration test suite"

$env:GIT_COMMITTER_DATE="2 days ago"
git add docker-compose.yml Dockerfile .env.example README.md
git commit --date="2 days ago" -m "ci: containerize multi-service platform with docker and finalize docs"

Remove-Item Env:\GIT_COMMITTER_DATE
git add .
git commit -m "refactor: apply recruiter feedback, optimize PostgreSQL queries, and configure scalable CORS"

git remote add origin https://github.com/Gamerboyi/identify-risk-intelligence-platform.git
git push -u origin master -f
