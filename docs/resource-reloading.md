# Hot reloading zasobów

Nowa infrastruktura pozwala na bezpieczne przeładowywanie plików konfiguracyjnych oraz zasobów XML bez konieczności restartu serwera. Mechanizm bazuje na dwóch prostych interfejsach i usłudze koordynującej proces na punktach bezpiecznych (po każdym przetworzeniu tury przez `RPServerManager`).

## Kluczowe klasy

| Klasa | Rola |
|-------|------|
| `ResourceProvider` | Abstrakcja nad źródłem danych. Dostarcza `InputStream` wskazanego zasobu. |
| `Reloadable` | Kontrakt dla komponentów, które potrafią odświeżyć swój stan na podstawie zasobu. |
| `ClassPathResourceProvider` | Dostawca korzystający z classpath aplikacji. |
| `PersistenceResourceProvider` | Domyślny dostawca korzystający z konfiguracji `ConfigurationParams`. |
| `ResourceReloadService` | Singleton zarządzający rejestracją i kolejką przeładowań. |

## Rejestracja komponentu

```java
public final class NpcTemplateRepository implements Reloadable {
	private final ResourceReloadService reloads;

	public NpcTemplateRepository(ResourceReloadService reloads) {
		this.reloads = reloads;
		reloads.register(this);
		reloads.requestReload(this); // wczytanie początkowe
	}

	@Override
	public String resourcePath() {
		return "data/npcs.xml";
	}

	@Override
	public void reload(ResourceProvider provider) throws Exception {
		try (InputStream in = provider.open(resourcePath())) {
			// wczytaj XML i zaktualizuj pamięć podręczną
		}
	}
}
```

> **Uwaga:** w kodzie należy używać tabulatorów zamiast spacji zgodnie z przyjętą konwencją.

## Dostępni dostawcy zasobów

* `PersistenceResourceProvider` – odczytuje pliki przez warstwę `Persistence`, dzięki czemu respektuje ustawienia `basedir` i `relativeToHome` wykorzystywane przy starcie serwera. `RPServerManager` konfiguruje go automatycznie, więc pliki w `data/` są odczytywane z aktualnej instalacji gry.
* `ClassPathResourceProvider` – sprawdza się przy zasobach spakowanych wewnątrz archiwów JAR.
* Własne implementacje `ResourceProvider` – np. odczyt z bazy danych lub zdalnego API.

Jeżeli projekt potrzebuje innego źródła danych, wystarczy przed rozpoczęciem przetwarzania tur ustawić własny dostawca:

```java
ResourceReloadService reloads = ResourceReloadService.getInstance();
reloads.setResourceProvider(new CustomResourceProvider(...));
```

## Kolejka przeładowań

1. Komponent wywołuje `ResourceReloadService.requestReload(...)` (np. po otrzymaniu sygnału zewnętrznego).
2. Żądanie trafia do bezpiecznej kolejki.
3. `RPServerManager` wykonuje `processPendingReloads()` po zakończeniu `world.nextTurn()` i przed rozpoczęciem następnej tury. Dzięki temu reload nie zakłóca przetwarzania akcji graczy.
4. Każdy komponent otrzymuje dostawcę zasobów i aktualizuje swój stan.

## Skrypt w repozytorium PolanieOnLine

Po stronie PolanieOnLine można przygotować skrypt, który:

1. Zbiera pliki do przeładowania (np. `*.xml`).
2. Wysyła do serwera komendę (REST/WebSocket/CLI) wskazującą zasób.
3. Serwer wywołuje `ResourceReloadService.requestReload(...)` dla odpowiedniego komponentu.

Dzięki temu serwer przeładuje dane w następnym bezpiecznym punkcie, bez konieczności restartu.
