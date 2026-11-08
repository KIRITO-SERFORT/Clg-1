"""Drop-in helper for DataVis (the DataVis source repository was not supplied).
Set CLG1_TRANSFER_API_BASE in the DataVis hosting environment to the Worker URL.
Call maybe_import_clg1_csv() before rendering the existing manual uploader.
"""
import os
import requests
import streamlit as st
import pandas as pd


def maybe_import_clg1_csv():
    cached = st.session_state.get("clg1_imported_df")
    if cached is not None:
        st.info("Imported from Clg 1% — review the selected academic data before analysis.")
        st.dataframe(cached.head(10), use_container_width=True)
        if st.button("Remove imported Clg 1% data"):
            st.session_state.pop("clg1_imported_df", None)
            st.rerun()
        return cached

    token = st.query_params.get("import_token")
    if not token:
        return None
    # Remove bearer token from browser URL immediately; never print or log it.
    try:
        del st.query_params["import_token"]
    except Exception:
        st.query_params.clear()
    base = os.environ.get("CLG1_TRANSFER_API_BASE", "").rstrip("/")
    if not base.startswith("https://"):
        st.error("Secure Clg 1% transfer is not configured. Use the regular CSV uploader.")
        return None
    try:
        response = requests.post(f"{base}/v1/transfers/redeem", json={"token": token}, timeout=15, allow_redirects=False)
        if response.status_code != 200 or not response.headers.get("Content-Type", "").lower().startswith("text/csv"):
            raise ValueError("Unsupported or expired transfer response")
        from io import StringIO
        frame = pd.read_csv(StringIO(response.text))
        required = {"schema_version", "record_type", "term_name", "subject_code", "subject_name"}
        if not required.issubset(frame.columns):
            st.error("The Clg 1% export has an unsupported column layout.")
            return None
        if not (frame["schema_version"].astype(str) == "1").all() or not (frame["record_type"].astype(str) == "assessment").all():
            st.error("Unsupported Clg 1% CSV schema version or record type.")
            return None
        st.session_state["clg1_imported_df"] = frame
        st.success("One-time Clg 1% transfer imported. The token can no longer be reused.")
        st.info("Imported from Clg 1% — review the selected academic data before analysis.")
        st.dataframe(frame.head(10), use_container_width=True)
        return frame
    except Exception:
        st.error("The Clg 1% transfer expired, was already used, or could not be retrieved. Use the manual uploader instead.")
        return None
