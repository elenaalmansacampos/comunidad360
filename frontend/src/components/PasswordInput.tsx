import { useId, useState } from "react";
import type { InputHTMLAttributes } from "react";
import { useTranslation } from "react-i18next";

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label: string;
  error?: string;
}

export function PasswordInput({ label, error, id, className = "", ...rest }: PasswordInputProps) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);
  const fallbackId = useId();
  const inputId = id ?? fallbackId;
  const errorId = `${inputId}-error`;

  return (
    <div className={`field ${className}`.trim()}>
      <label className="field__label" htmlFor={inputId}>
        {label}
      </label>
      <div className="field__password">
        <input
          id={inputId}
          type={visible ? "text" : "password"}
          className={`field__input field__input--password ${error ? "field__input--error" : ""}`.trim()}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          {...rest}
        />
        <button
          type="button"
          className="field__password-toggle"
          onClick={() => setVisible((value) => !value)}
          aria-label={t(visible ? "common.hidePassword" : "common.showPassword")}
          aria-pressed={visible}
          aria-controls={inputId}
          title={t(visible ? "common.hidePassword" : "common.showPassword")}
        >
          {visible ? (
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="m3 3 18 18" />
              <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
              <path d="M9.9 5.2A10.6 10.6 0 0 1 12 5c6.5 0 10 7 10 7a16.8 16.8 0 0 1-3.1 4.1" />
              <path d="M6.2 6.2C3.5 8 2 12 2 12s3.5 7 10 7a10.9 10.9 0 0 0 4.1-.8" />
            </svg>
          ) : (
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          )}
        </button>
      </div>
      {error ? (
        <p className="field__error" id={errorId}>
          {error}
        </p>
      ) : null}
    </div>
  );
}
