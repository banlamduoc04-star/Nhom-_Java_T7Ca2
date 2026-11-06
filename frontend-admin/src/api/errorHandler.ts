import { notification } from 'antd';

export interface ApiErrorResponse {
  errorCode: string;
  message: string;
  details?: any[];
}

export const handleApiError = (error: any) => {
  const data: ApiErrorResponse = error?.response?.data || error;
  const errorCode = data?.errorCode || 'SY50001';
  const errorMessage = data?.message || 'Có lỗi xảy ra, vui lòng thử lại sau.';

  notification.error({
    message: `Lỗi thao tác [${errorCode}]`,
    description: errorMessage,
    placement: 'topRight',
    duration: 4,
  });
};