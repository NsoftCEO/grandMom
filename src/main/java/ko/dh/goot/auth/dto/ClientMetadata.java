package ko.dh.goot.auth.dto;

/**
 * 클라이언트(기기 및 네트워크) 메타데이터
 */
public record ClientMetadata(
        String ipAddress,
        String userAgent,
        String deviceId,
        String deviceType,
        String deviceName
) {
}