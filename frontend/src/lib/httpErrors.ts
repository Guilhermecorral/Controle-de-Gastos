import axios from 'axios'

type ApiErrorPayload = {
  message?: string
  details?: string[]
}

/**
 * Traduz erros da API para uma mensagem curta e útil para a interface.
 */
export function getApiErrorMessage(error: unknown, fallbackMessage: string) {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return fallbackMessage
  }

  const responseData = error.response?.data

  if (responseData?.details && responseData.details.length > 0) {
    return responseData.details[0]
      .replace(/^password:\s*/i, 'Senha: ')
      .replace(/^confirmPassword:\s*/i, 'Confirmação da senha: ')
      .replace(/^email:\s*/i, 'E-mail: ')
      .replace(/^name:\s*/i, 'Nome: ')
  }

  if (responseData?.message) {
    return responseData.message
  }

  return fallbackMessage
}
