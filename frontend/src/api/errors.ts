import { AxiosError } from "axios";

/** Backend ErrorResponse body shape. */
interface ApiErrorBody {
  status?: number;
  error?: string;
  code?: string;
  message?: string;
  path?: string;
  fieldErrors?: { field: string; message: string }[];
}

/**
 * Turns any error thrown by an axios request into a human-readable message,
 * preferring the backend's ErrorResponse.message and field errors.
 */
export function getApiErrorMessage(err: unknown, fallback = "Something went wrong. Please try again."): string {
  const axiosErr = err as AxiosError<ApiErrorBody>;
  const body = axiosErr?.response?.data;
  if (body) {
    if (body.fieldErrors && body.fieldErrors.length > 0) {
      return body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(", ");
    }
    if (body.message) {
      return body.message;
    }
  }
  if (axiosErr?.message) {
    return axiosErr.message;
  }
  return fallback;
}
