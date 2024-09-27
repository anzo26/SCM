"use client";

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
    faUsers,
    faTrash,
    faArrowLeft,
    faPlus,
    faExclamationTriangle,
    faDumpster, faHouse, faAddressBook, faCodeMerge
} from "@fortawesome/free-solid-svg-icons";
import { toast } from "react-toastify";
import EditTenantPopup from "@/Components/Tenant/EditTenantPopup";
import AddNewContactPopup from "@/Components/Contact/AddNewContactPopup";
import TenantSettingsPopup from "@/Components/Tenant/TenantSettingsPopup";
import React, { useState } from "react";
import { Tenant as TenantModel } from "@/models/Tenant";
import { useRouter } from "next/navigation";
import Link from "next/link";
import TenantPopup from "@/Components/Tenant/TenantPopup";
import ImportContacts from "@/Components/Contact/ImportContacts";

interface TenantInfoDisplayProps {
    tenant: TenantModel;
    contactsNumber: number;
    IdToken: string;
    onSave: () => void;
    numberOfTenants: number;
}

const TenantInfoDisplay: React.FC<TenantInfoDisplayProps> = (props) => {
    const router = useRouter();
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [confirmationText, setConfirmationText] = useState("");
    const [isDeleteDisabled, setIsDeleteDisabled] = useState(true);
    const [requestLoading, setRequestLoading] = useState<boolean>(false);

    const handleDeleteTenant = async () => {
        setRequestLoading(true);
        try {
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tenants/deactivate/${props.tenant.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'userToken': `Bearer ${document.cookie.split('IdToken=')[1]}`,
                },
            });

            if (!res.ok) {
                toast.error(res.statusText || "Failed to delete tenant!");
            }

            toast.success("Tenant deleted successfully!");
            setRequestLoading(false);
            router.push('/');
        } catch (error: any) {
            toast.error(error.message || "Failed to delete tenant.");
            setRequestLoading(false);
        }
    };

    const handleTenantAdd = async () => {
        router.push('/');
    }

    const handleConfirmationTextChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setConfirmationText(event.target.value);
        setIsDeleteDisabled(event.target.value !== props.tenant.title);
    };

    return (
        <div>
            <div className="flex justify-between mb-4">
                <div>
                    <div className={"flex items-center"}>
                        {props.numberOfTenants > 1 && (
                            <div className="flex items-center mb-4">
                                <FontAwesomeIcon
                                    icon={faArrowLeft}
                                    className="text-primary mr-4 mt-4 cursor-pointer w-3.5 h-auto hover:scale-105 transition"
                                    onClick={() => {
                                        router.push("/");
                                        router.refresh();
                                    }}
                                />
                            </div>
                        )}
                        <div className="text-sm breadcrumbs mx-2">
                            <ul className={"text-gray-500"}>
                                <li><Link href={"/"}><FontAwesomeIcon icon={faHouse} className={"mr-1"}/>Home</Link></li>
                                <li><Link href={"#"}><FontAwesomeIcon icon={faAddressBook} className={"mr-1"}/>{props.tenant.title}</Link></li>
                            </ul>
                        </div>
                        <div className="ml-5">
                            <TenantPopup onTenantAdd={() => handleTenantAdd()} IdToken={props.IdToken} icon={faPlus}
                                         buttonAction={"Tenant"} title={"Add new Tenant"}
                                         labels={["Title", "Description", "Colour", "Other users"]}
                            />
                        </div>
                    </div>
                    <div className={"flex items-center"}>
                        <h2 className="text-3xl font-semibold text-primary">{props.tenant.title}</h2>
                        <span className="ml-6 mr-2 text-2xl font-semibold ">{props.contactsNumber}</span>
                        <FontAwesomeIcon className="h-5 w-auto mb-0.5 " icon={faUsers} />
                    </div>
                    <p className="text-m text-gray-600 mt-1 break-words max-w-96">{props.tenant.description}</p>
                    <div className="mt-4 flex items-center space-x-3">
                        <EditTenantPopup tenant={props.tenant} />
                        <TenantSettingsPopup tenant={props.tenant} IdToken={props.IdToken} />
                        <button
                            onClick={() => setShowConfirmation(true)}
                            className="btn px-4 btn-sm bg-red-600 border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-red-700">
                            Delete Tenant <FontAwesomeIcon className="ml-1 w-3.5 h-auto" icon={faTrash} />
                        </button>
                    </div>
                </div>
                <div className="flex items-end space-x-3">
                    <div className={"flex items-center"}>
                        <Link href={`/contacts/${props.tenant.tenantUniqueName}/deleted`} className={"hover:scale-105 transition text-danger dark:text-white text-xs mr-3"}>
                            Deleted Contacts
                            <FontAwesomeIcon className={"ml-1 w-3 h-auto"} icon={faDumpster} />
                        </Link>
                        <AddNewContactPopup tenantUniqueName={props.tenant.tenantUniqueName} onSave={props.onSave} duplicate={false}/>
                        <ImportContacts tenantUniqueName={props.tenant.tenantUniqueName} IdToken={props.IdToken} />
                        <Link href={`/contacts/${props.tenant.tenantUniqueName}/merge`} className={"btn px-4 btn-sm bg-primary border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-primary ml-3"}>
                            Merge Contacts
                            <FontAwesomeIcon className={"ml-1 w-3 h-auto"} icon={faCodeMerge} />
                        </Link>
                    </div>
                </div>
            </div>
            {showConfirmation && (
                <div className="fixed z-20 flex flex-col justify-center items-center bg-gray-500 bg-opacity-65 inset-0">
                    <div className="bg-white p-8 rounded-lg shadow-lg relative z-50">
                        <h2 className="text-xl mb-4 font-semibold flex items-center">
                            Are you sure you want to delete this tenant?
                            <FontAwesomeIcon className="ml-2 w-5 h-5 text-red-600" icon={faExclamationTriangle}/>
                        </h2>
                        <p>
                            This action will permanently delete the tenant and all its data from the database.<br/>
                            No users will be able to access this tenant anymore.
                        </p>
                        <p className={"mt-3"}>Type <strong>{props.tenant.title}</strong> to confirm.</p>
                        <input
                            type="text"
                            className="mt-2 mb-4 p-2 border rounded w-full"
                            value={confirmationText}
                            onChange={handleConfirmationTextChange}
                        />
                        <div className="flex justify-end">
                            <button
                                onClick={() => setShowConfirmation(false)}
                                className="px-4 py-1 rounded-8 bg-gray-300 text-black mr-2 font-semibold disabled:opacity-50 hover:scale-105 transition">
                                Cancel
                            </button>
                            {requestLoading ? (
                                <span className="loading loading-spinner text-primary"></span>
                            ) : (
                                <button
                                    onClick={handleDeleteTenant}
                                    className="btn px-4 py-1 btn-sm bg-red-600 border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-danger"
                                    disabled={isDeleteDisabled}>
                                    Delete
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default TenantInfoDisplay;
