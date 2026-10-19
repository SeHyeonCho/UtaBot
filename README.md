# 우타 봇

Discord JDA + LavaPlayer 기반 음악 재생 우타 봇입니다.  
Slash Command 기반으로 명령어를 수행합니다.

----

# 기능 목록

## 음악 재생 기능
- `/play <검색어 | URL>`  
  - 유튜브 URL 또는 검색어로 음악을 로드
  - Lavaplayer의 `youtube-source`를 통해 YouTube URL을 처리하여 재생
  - 자동으로 사용자의 음성 채널에 접속하여 재생
- `/skip` — 현재 트랙 건너뛰기
- `/stop` — 재생 중지 + 대기열 초기화
- `/queue` — 현재 대기열 표시
- `/volume [level]` — 볼륨 조절 (0~150, 비우면 현재 볼륨 표시)

---

## 대기열 관리 
- 여러 트랙 대기열 추가 가능
- 현재 곡이 끝나면 자동으로 다음 곡 재생

---

## 입장곡(Entry Song) 기능

입장곡은 **서버별로** 따로 설정됩니다. 같은 유저라도 서버마다 다른 입장곡을 쓸 수 있습니다.

### 1. 업로드 채널 기반 mp3 입장곡
- `/setuploadchannel <채널>`  
  - 서버별로 **mp3 업로드 전용 텍스트 채널**을 설정
- 설정된 업로드 채널에 `.mp3` 파일을 올리면:
  - 서버 로컬 `uploads/{서버ID}/{유저ID}.mp3` 로 자동 저장
  - 그 서버에서 해당 유저의 입장곡으로 자동 등록
  - 기본 재생 구간: `0초 ~ 10초`

### 2. 유튜브 URL 기반 입장곡
- `/setentrysong <url>`  
  - 유튜브 URL을 그 서버의 **입장곡으로 설정**
  - 내부적으로 Lavaplayer의 `youtube-source`를 통해 YouTube URL을 처리하여 재생
  - `music.youtube.com`, `youtu.be` 주소도 처리 가능
  - 기본 재생 구간: `0초 ~ 7초`

### 3. 입장곡 재생 구간 설정
- `/setentrytime <start> <duration>`  
  - 그 서버에 설정된 내 입장곡(로컬 mp3 또는 URL)에 대해  
    재생 시작 지점과 재생 길이(초)를 설정
  - 예: `/setentrytime start:5 duration:7`  
    → 5초부터 7초 동안 재생

### 4. 입장 시 동작 방식
- 유저가 음성 채널에 **입장**하면:
  1. (서버 ID, 유저 ID)로 입장곡 설정 조회
     - URL이면 → Lavaplayer의 `youtube-source`를 통해 재생  
     - 파일이면 → `uploads/{서버ID}/{유저ID}.mp3` 재생
  2. 봇이 **다른 채널에서 음악을 재생 중이면** 듣는 사람들을 위해 이동하지 않고 입장곡을 건너뜀
  3. 현재 재생 중인 트랙이 있으면:
     - 현재 트랙을 멈춘 위치를 기억하고 입장곡을 재생
     - 지정된 duration 이후 원래 트랙을 멈췄던 위치부터 다시 재생
     - 원래 트랙이 없었으면 대기열의 다음 곡을 재생
  4. 입장곡이 끝나기 전에 다른 사람이 들어오면 입장곡만 교체되고, 마지막 입장곡이 끝나면 원래 트랙으로 돌아감

---

## 설정 및 상태 저장

- 입장곡 설정 정보는 `EntrySongRepository`(`entrysong.repository.EntrySongRepository`)를 통해 관리
  - 키: 서버 ID → 유저 ID
  - 값: `source`(업로드 파일명 또는 URL), `startSec`, `durationSec`
- 설정 정보는 JSON 파일(`data/server-entry-songs.json`)에 저장되며:
  - 봇 시작 시 디스크에서 로드
  - `/setentrysong`, `/setentrytime`, mp3 업로드 시마다 자동으로 저장
  - 서버를 껐다 켜도 **입장곡 설정 유지**

- 업로드 채널 정보는 `UploadChannelRepository`(`entrysong.repository.UploadChannelRepository`)에서 서버별로 관리
  - 서버 ID → 업로드 채널 ID 매핑
  - 메모리 내에서 관리 (재시작 시 초기화)

### 예전 입장곡 데이터 이전
예전 버전은 입장곡을 `username#discriminator` 기준으로 모든 서버에 공통으로 저장했습니다
(`data/entry-songs.json`, `uploads/username#0000.mp3`).
봇을 시작할 때 이 데이터가 남아 있으면 자동으로 새 구조로 옮깁니다.

- 봇이 들어가 있는 각 서버에서 이름이 일치하는 멤버를 찾아, **그 유저가 속한 모든 서버**에 입장곡을 복사
- 새 구조에 이미 설정이 있는 유저는 덮어쓰지 않음
- 옮긴 뒤 예전 파일은 `data/legacy/`, `uploads/legacy/` 로 옮겨 백업 (확인 후 지워도 됨)
- 주인을 찾지 못한 항목(서버를 떠난 유저 등)은 로그에 출력되고 백업 폴더에만 남음

---

## 프로젝트 구조

```
src/main/java/
├── Main.java                          # 봇 진입점 (객체 생성/연결, 예전 데이터 이전)
├── common/                            # 공통 기능
│   ├── command/
│   │   ├── SlashCommand.java          # 명령어 정의 + 처리 인터페이스
│   │   ├── CommandDispatcher.java     # 서버에 명령어 등록, 이름으로 찾아 실행
│   │   └── basic/                     # 기본 명령어 (ping, echo)
│   └── util/
│       └── CommandUtils.java          # 서버/음성 채널 확인 등 명령어 유틸리티
├── music/                             # 음악 재생 기능
│   ├── audio/
│   │   ├── AudioPlayerManagerFactory.java # LavaPlayer 설정, youtube-source 등록
│   │   └── AudioPlayerSendHandler.java    # LavaPlayer 오디오를 Discord로 전송
│   ├── command/                       # join, leave, play, skip, stop, queue, volume
│   └── service/
│       ├── MusicManager.java          # 서버별 음악 매니저 관리
│       ├── ServerMusicManager.java    # 서버별 재생 제어 + 음성 연결
│       └── TrackScheduler.java        # 트랙 대기열, 입장곡 끼워넣기
└── entrysong/                         # 입장곡 기능
    ├── command/                       # setentrysong, setentrytime, setuploadchannel
    ├── handler/
    │   ├── EntrySongJoinHandler.java  # 음성 채널 입장 시 입장곡 재생
    │   └── EntrySongUploadHandler.java # 업로드 채널의 mp3 저장
    ├── model/
    │   └── EntrySongConfig.java       # 입장곡 설정 데이터 클래스
    ├── repository/
    │   ├── EntrySongRepository.java   # 서버별 입장곡 저장소 (JSON 저장/로드)
    │   └── UploadChannelRepository.java # 서버별 업로드 채널 저장소
    └── service/
        ├── EntrySongService.java      # 입장곡 설정, 업로드 파일 규칙
        ├── EntrySongSource.java       # 입장 시 재생할 입장곡 정보
        └── LegacyEntrySongMigration.java # 예전 입장곡 데이터 이전
```

### 명령어 추가 방법
1. `SlashCommand`를 구현하는 클래스를 만들고 `data()`에 이름/설명/옵션을, `execute()`에 처리를 작성
2. `Main`의 `CommandDispatcher` 목록에 추가 (봇이 켜질 때 모든 서버에 자동 등록)

---

## 테스트

- `./gradlew test` — 단위 테스트. Discord 토큰이나 네트워크 없이 실행됩니다.
- `./gradlew integrationTest` — 실제 Discord 봇 토큰(`.env`의 `TOKEN`)이나 YouTube 접속이 필요한 테스트.
  토큰이 없으면 해당 테스트는 건너뜁니다.

---

# 추가 예정 기능

## Loop & Playback 기능
- `/loop` — 현재 트랙 반복
- `/loopqueue` — 전체 대기열 반복
- `/shuffle` — 대기열 랜덤 섞기  

---

## 반응형 UI 기능 
- 메시지에 음악 제어 버튼 표시  
  (⏯, ⏭, ⏹, 🔀, 🔁 등)
- Slash 없이도 조작 가능한 UI

---

## 가사(Lyrics) 출력
- `/lyrics` → 현재 재생 트랙의 가사 자동 검색

---