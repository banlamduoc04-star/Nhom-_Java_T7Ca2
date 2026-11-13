import { notification } from 'antd';
import type { AxiosError } from 'axios';

export interface ApiErrorResponse {
  errorCode: string;
  message: string;
  details?: { field: string; message: string }[];
}

export const handleApiError = (error: unknown) => {
  const data: Partial<ApiErrorResponse> | undefined = (error as AxiosError<ApiErrorResponse>)?.response?.data;
  const errorCode = data?.errorCode || 'SY50001';
  const errorMessage = data?.message || 'Có lỗi xảy ra, vui lòng thử lại sau.';

  notification.error({
    message: `Lỗi thao tác [${errorCode}]`,
    description: errorMessage,
    placement: 'topRight',
    duration: 4,
  });
};