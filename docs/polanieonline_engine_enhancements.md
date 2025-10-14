# Pomysły na ulepszenia silnika Marauroa dla PolanieOnLine

## 1. Warstwa zdarzeń i skryptów
- **Cel**: umożliwienie tworzenia złożonych misji, zdarzeń sezonowych i scenariuszy społecznościowych bez potrzeby rekompilacji serwera.
- **Zmiany w Marauroa**:
  - Wprowadzenie modułu zdarzeń opartych na harmonogramie (cron-like) oraz wyzwalaczach związanych z akcjami graczy.
  - Dodanie obsługi skryptów (np. JSR-223) z kontrolą bezpieczeństwa sandbox i dostępem do API gry.
- **Korzyści dla PolanieOnLine**: szybsze wdrażanie wydarzeń inspirowanych historią Polan i reagowanie na aktywność społeczności.

## 2. System stanów świata i sezonowości
- **Cel**: utrzymanie długofalowych konsekwencji działań graczy (np. przejmowanie grodów, stan zasobów wioski).
- **Zmiany w Marauroa**:
  - Wprowadzenie modułu persystencji stanów sezonowych (np. osobna tabela w bazie) oraz API do ich modyfikacji.
  - Mechanizm snapshotów świata pozwalający na łatwe cofnięcie w przypadku błędów.
- **Korzyści dla PolanieOnLine**: możliwość prowadzenia kampanii sezonowych z resetem części progresu, zachowując dorobek graczy.

## 3. Integracja z mikroserwisami i REST/gRPC
- **Cel**: umożliwienie współpracy serwera z zewnętrznymi usługami (np. rankingami, płatnościami, analityką).
- **Zmiany w Marauroa**:
  - Dodanie warstwy komunikacji (REST lub gRPC) z kontrolą autoryzacji.
  - System webhooków do wysyłania zdarzeń o działaniach graczy.
- **Korzyści dla PolanieOnLine**: możliwość integracji z portalem społecznościowym gry i zewnętrznymi usługami.

## 4. Usprawniony model AI NPC
- **Cel**: tworzenie inteligentniejszych NPC wspierających narrację i ekonomię gry.
- **Zmiany w Marauroa**:
  - Warstwa behawioralna oparta o drzewa zachowań lub GOAP.
  - Interfejs danych konfiguracyjnych w formacie JSON/ YAML dla łatwej edycji.
- **Korzyści dla PolanieOnLine**: bogatsze interakcje z NPC, strażnicy grodów, kupcy reagujący na popyt i podaż.

## 5. System ekonomii i produkcji zasobów
- **Cel**: odzwierciedlenie gospodarki wczesnopiastowskiej w świecie gry.
- **Zmiany w Marauroa**:
  - Moduł gospodarczy zarządzający produkcją, konsumpcją i handlem zasobami.
  - Narzędzia do monitorowania inflacji i regulacji kursów wymiany.
- **Korzyści dla PolanieOnLine**: zbalansowana ekonomia, możliwość wprowadzania podatków klanowych i systemów cechowych.

## 6. Narzędzia administracyjne i monitorowanie
- **Cel**: lepsza kontrola jakości i stabilności serwera.
- **Zmiany w Marauroa**:
  - Panel monitorowania (np. JMX + exporter Prometheus) i dashboardy.
  - System alertów w przypadku błędów logicznych lub przeciążeń.
- **Korzyści dla PolanieOnLine**: proaktywne reagowanie na problemy, dane do balansowania mechanik.

## 7. Wsparcie dla klienta webowego
- **Cel**: umożliwienie szybkiego wejścia do gry bez instalacji klienta.
- **Zmiany w Marauroa**:
  - Warstwa komunikacji WebSocket z translacją protokołu gry.
  - Mechanizmy ograniczania przepustowości i kompresji danych.
- **Korzyści dla PolanieOnLine**: zwiększenie dostępności gry i łatwiejsze testy społecznościowe.

## 8. Pipeline CI/CD dla wtyczek gry
- **Cel**: bezpieczne wdrażanie aktualizacji bez przestojów.
- **Zmiany w Marauroa**:
  - Standaryzacja struktury modułów rozszerzeń i skryptów.
  - Integracja z narzędziami budującymi (np. Gradle, Maven) oraz testami jednostkowymi/integracyjnymi.
- **Korzyści dla PolanieOnLine**: szybsze dostarczanie nowych funkcji przy zachowaniu jakości.

## 9. Dokumentacja i przykładowe moduły
- **Cel**: ułatwienie onboarding nowych deweloperów.
- **Zmiany w Marauroa**:
  - Rozszerzenie dokumentacji o przewodnik integracyjny dla PolanieOnLine.
  - Zestaw przykładowych modułów (event, ekonomia, AI) jako baza startowa.
- **Korzyści dla PolanieOnLine**: skrócenie czasu potrzebnego na wdrożenie zespołu i prototypowanie.

---

Implementacja powyższych funkcji może być realizowana iteracyjnie. Rekomenduje się rozpoczęcie od warstwy zdarzeń i dokumentacji API, aby umożliwić szybsze prototypowanie funkcji specyficznych dla PolanieOnLine.
