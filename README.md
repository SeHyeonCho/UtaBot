# 우타 봇

Discord JDA + LavaPlayer 기반 음악 재생 우타 봇입니다.  
Slash Command 기반으로 명령어를 수행합니다.

----

# 기능 목록

## 음악 재생 기능
- `/play <검색어 | URL>`  
  - 유튜브 URL 또는 검색어로 음악을 로드
  - `AudioUrlResolver`를 통해 `yt-dlp`로 실제 오디오 스트림 URL을 resolve 한 뒤 재생
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

### 1. 업로드 채널 기반 mp3 입장곡
- `/setuploadchannel <채널>`  
  - 서버별로 **mp3 업로드 전용 텍스트 채널**을 설정
- 설정된 업로드 채널에 `.mp3` 파일을 올리면:
  - 서버 로컬 `uploads/` 폴더에  
    `username#discriminator.mp3` 형식으로 자동 저장  
    (예: `우타#2960.mp3`)
  - 해당 유저의 입장곡으로 자동 등록
  - 기본 재생 구간: `0초 ~ 10초`

### 2. 유튜브 URL 기반 입장곡
- `/setentrysong <url>`  
  - 유튜브(또는 기타 지원 URL)를 **입장곡으로 설정**
  - 내부적으로 `AudioUrlResolver`를 통해 `yt-dlp`로 실제 오디오 스트림 URL을 resolve 한 뒤 재생
  - `music.youtube.com`, `youtu.be` 등도 정규화하여 처리 가능

### 3. 입장곡 재생 구간 설정
- `/setentrytime <start> <duration>`  
  - 현재 유저의 입장곡(로컬 mp3 또는 URL)에 대해  
    재생 시작 지점과 재생 길이(초)를 설정
  - 예: `/setentrytime start:5 duration:7`  
    → 5초부터 7초 동안 재생

### 4. 입장 시 동작 방식
- 유저가 음성 채널에 **입장**하면:
  1. 해당 유저의 `username#discriminator` 키로 EntrySongRegistry 조회
  2. 우선 **레지스트리에 등록된 입장곡** 사용  
     - URL이면 → `AudioUrlResolver`를 통해 `yt-dlp`로 스트림 URL resolve 후 재생  
     - 파일명이면 → `uploads/파일명` 에서 mp3 재생
  3. 레지스트리에 없으면  
     `uploads/{username}#{discriminator}.mp3` 가 존재할 경우 이를 fallback 입장곡으로 사용
  4. 입장곡 재생 전, 현재 재생 중인 트랙이 있으면:
     - 현재 트랙을 clone + position 저장
     - 입장곡을 **덮어쓰기 모드로 재생**
     - 지정된 duration 이후:
       - 입장곡 정지
       - 이전에 백업해둔 트랙을 원래 위치에서 다시 재생

---

## 설정 및 상태 저장

- 입장곡 설정 정보는 `EntrySongRegistry`(`entrysong.repository.EntrySongRegistry`)를 통해 관리
  - 키: `username#discriminator`
  - 값: `fileName(또는 URL)`, `startSec`, `durationSec`
- 설정 정보는 JSON 파일(`data/entry-songs.json`)에 저장되며:
  - 봇 시작 시 디스크에서 로드
  - `/setentrysong`, `/setentrytime`, mp3 업로드 시마다 자동으로 저장
  - 서버를 껐다 켜도 **입장곡 설정 유지**

- 업로드 채널 정보는 `UploadChannelRegistry`(`upload.repository.UploadChannelRegistry`)에서 서버별로 관리
  - 서버 ID → 업로드 채널 ID 매핑
  - 메모리 내에서 관리 (재시작 시 초기화)

---

## 프로젝트 구조

```
src/main/java/
├── Main.java                          # 봇 진입점
├── common/                            # 공통 기능
│   ├── command/
│   │   └── CommandRegister.java      # 슬래시 명령어 등록
│   ├── handler/
│   │   ├── DefaultCommandHandler.java # 기본 명령어 (ping, echo)
│   │   └── VoiceCommandHandler.java  # 보이스 채널 명령어 (join, leave)
│   └── util/
│       ├── AudioUrlResolver.java      # yt-dlp를 통한 오디오 URL 해석
│       └── CommandUtils.java         # 명령어 유틸리티
├── music/                             # 음악 재생 기능
│   ├── handler/
│   │   ├── MusicCommandHandler.java   # 음악 명령어 (play, skip, stop, queue, volume)
│   │   └── AudioPlayerHandler.java   # LavaPlayer 오디오 전송 핸들러
│   └── service/
│       ├── MusicManager.java          # 서버별 음악 매니저 관리
│       ├── ServerMusicManager.java   # 서버별 음악 재생 구성 요소
│       └── TrackScheduler.java       # 트랙 대기열 관리
├── entrysong/                         # 입장곡 기능
│   ├── handler/
│   │   └── EntrySongHandler.java      # 입장곡 명령어 및 이벤트 처리
│   └── repository/
│       ├── EntrySongConfig.java       # 입장곡 설정 데이터 클래스
│       └── EntrySongRegistry.java     # 입장곡 레지스트리 (JSON 저장/로드)
└── upload/                            # 파일 업로드 기능
    ├── handler/
    │   └── UploadHandler.java         # mp3 업로드 채널 처리
    └── repository/
        └── UploadChannelRegistry.java # 업로드 채널 레지스트리
```

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